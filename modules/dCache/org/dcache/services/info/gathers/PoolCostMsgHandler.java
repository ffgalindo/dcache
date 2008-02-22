package org.dcache.services.info.gathers;

import org.dcache.services.info.InfoProvider;

import diskCacheV111.vehicles.CostModulePoolInfoTable;
import diskCacheV111.pools.PoolCostInfo;
import diskCacheV111.pools.PoolCostInfo.PoolQueueInfo;
import diskCacheV111.pools.PoolCostInfo.PoolSpaceInfo;
import diskCacheV111.pools.PoolCostInfo.NamedPoolQueueInfo;

import java.util.*;

import org.dcache.services.info.base.*;

/**
 * This class processing incoming CellMessages that contain CostModulePoolInfoTable
 * 
 * @author Paul Millar <paul.millar@desy.de>
 */
public class PoolCostMsgHandler extends CellMessageHandlerSkel {

	public void process(Object msgPayload, long metricLifetime) {
		
		if( !(msgPayload instanceof CostModulePoolInfoTable)) {
			InfoProvider.getInstance().say("received non-CostModulePoolInfoTable object in message\n");
			return;
		}

		CostModulePoolInfoTable poolInfoTbl = (CostModulePoolInfoTable) msgPayload;
		
		Collection<PoolCostInfo> poolInfos = poolInfoTbl.poolInfos();
		StatePath poolsPath = new StatePath("pools");
		
		AppendableStateUpdate update = new AppendableStateUpdate();
		
		for( PoolCostInfo thisPoolInfo : poolInfos) {
			
			String poolName = thisPoolInfo.getPoolName();
			
			StatePath pathToThisPool = poolsPath.newChild(poolName); 
			StatePath pathToQueues = pathToThisPool.newChild("queues");

			
			/**
			 *  Add all the standard queues
			 */
			
			addQueueInfo( update, pathToQueues, "store", thisPoolInfo.getStoreQueue(), metricLifetime);
			addQueueInfo( update, pathToQueues, "restore", thisPoolInfo.getRestoreQueue(), metricLifetime);
			addQueueInfo( update, pathToQueues, "mover", thisPoolInfo.getMoverQueue(), metricLifetime);
			addQueueInfo( update, pathToQueues, "p2p-queue", thisPoolInfo.getP2pQueue(), metricLifetime);
			addQueueInfo( update, pathToQueues, "p2p-clientqueue", thisPoolInfo.getP2pClientQueue(), metricLifetime);
			
			
			/**
			 *  Add the "extra" named queues
			 */
			
			addNamedQueues( update, pathToQueues, thisPoolInfo, metricLifetime);


			/**
			 *  Add information about our default queue's name, if we have one.
			 */
			
			String defaultQueue = thisPoolInfo.getDefaultQueueName();
			if( defaultQueue != null)
				update.appendUpdate(pathToQueues.newChild("default-queue"), 
						new StringStateValue( defaultQueue, metricLifetime));

			
			/**
			 *  Add information about this pool's space utilisation.
			 */
			
			addSpaceInfo( update, pathToThisPool.newChild("space"), thisPoolInfo.getSpaceInfo(), metricLifetime);
		}
		
		applyUpdates( update);
	}
	
	
	
	/**
	 * Add information about a specific queue to a pool's portion of dCache state.
	 * The state tree looks like:
	 * 
	 * <pre>
	 * [dCache]
	 *  |
	 *  +--[pools]
	 *  |   |
	 *  |   +--[&lt;poolName>]
	 *  |   |   |
	 *  |   |   +--[queues]
	 *  |   |   |   |
	 *  |   |   |   +--[&lt;queueName1>]
	 *  |   |   |   |    |
	 *  |   |   |   |    +--active: nnn
	 *  |   |   |   |    +--max-active: nnn
	 *  |   |   |   |    +--queued: nnn
	 *  |   |   |   |
	 *  |   |   |   +--[&lt;queueName2>]
	 * </pre>
	 * 
	 * @param pathToQueues the StatePath pointing to queues (e.g.,
	 * "pools.mypool_1.queues")
	 * @param queueName the name of the queue.
	 */
	private void addQueueInfo( AppendableStateUpdate stateUpdate, StatePath pathToQueues,
								String queueName, PoolQueueInfo info, long lifetime) {		
		StatePath queuePath = pathToQueues.newChild(queueName);
		
		stateUpdate.appendUpdate(queuePath.newChild("active"), 
					new IntegerStateValue(info.getActive(), lifetime));
		stateUpdate.appendUpdate(queuePath.newChild("max-active"), 
				new IntegerStateValue(info.getMaxActive(), lifetime));
		stateUpdate.appendUpdate(queuePath.newChild("queued"), 
				new IntegerStateValue(info.getQueued(), lifetime));
	}
	
	
	/**
	 * Adds information from a pool's PoolSpaceInfo object. 
	 * We add this into the state in the following way:
	 * 
	 * <pre>
	 * [dCache]
	 *  |
	 *  +--[pools]
	 *  |   |
	 *  |   +--[&lt;poolName>]
	 *  |   |   |
	 *  |   |   +--[space]
	 *  |   |   |   |
	 *  |   |   |   +--total: nnn
	 *  |   |   |   +--free: nnn
	 *  |   |   |   +--precious: nnn
	 *  |   |   |   +--removable: nnn
	 *  |   |   |   +--gap: nnn
	 *  |   |   |   +--break-even: nnn
	 *  |   |   |   +--LRU-seconds: nnn
	 * </pre>
	 *
	 * @param stateUpdate the StateUpdate we will append
	 * @param path the StatePath pointing to the space branch
	 * @param info the space information to include.
	 */
	private void addSpaceInfo( AppendableStateUpdate stateUpdate, StatePath pathToSpace, 
							PoolSpaceInfo info, long lifetime) {
		stateUpdate.appendUpdate( pathToSpace.newChild("total"),
					new IntegerStateValue( info.getTotalSpace(), lifetime));
		stateUpdate.appendUpdate( pathToSpace.newChild("free"),
				new IntegerStateValue( info.getFreeSpace(), lifetime));
		stateUpdate.appendUpdate( pathToSpace.newChild("precious"),
				new IntegerStateValue( info.getPreciousSpace(), lifetime));
		stateUpdate.appendUpdate( pathToSpace.newChild("removable"),
				new IntegerStateValue( info.getRemovableSpace(), lifetime));
		stateUpdate.appendUpdate( pathToSpace.newChild("gap"),
				new IntegerStateValue( info.getGap(), lifetime));
		stateUpdate.appendUpdate( pathToSpace.newChild("break-even"),
				new FloatingPointStateValue( info.getBreakEven(), lifetime));
		stateUpdate.appendUpdate( pathToSpace.newChild("LRU-seconds"),
				new IntegerStateValue( info.getLRUSeconds(), lifetime));
	}
	
	
	/**
	 * Add information about all "named" queues.  The available information is the
	 * same as with regular queues, but there are arbirary number of these. The
	 * information is presented underneath the named-queues branch of the queues
	 * branch:
	 * 
	 * <pre>
	 * [dCache]
	 *  |
	 *  +--[pools]
	 *  |   |
	 *  |   +--[&lt;poolName>]
	 *  |   |   |
	 *  |   |   +--[queues]
	 *  |   |   |   |
	 *  |   |   |   +--[named-queues]
	 *  |   |   |   |   |
	 *  |   |   |   |   +--[&lt;namedQueue1>]
	 *  |   |   |   |   |    |
	 *  |   |   |   |   |    +--active: nnn
	 *  |   |   |   |   |    +--max-active: nnn
	 *  </pre>
	 *  
	 * @param update the StateUpdate we are appending to
	 * @param pathToQueues the StatePath pointing to [queues] above
	 * @param thisPoolInfo the information about this pool.
	 */
	private void addNamedQueues( AppendableStateUpdate update, StatePath pathToQueues, PoolCostInfo thisPoolInfo, long lifetime)	{
		Map<String, NamedPoolQueueInfo> namedQueuesInfo = thisPoolInfo.getExtendedMoverHash();

		if( namedQueuesInfo == null)
			return;
		
		StatePath pathToNamedQueues = pathToQueues.newChild("named-queues");	
		
		for( Iterator<NamedPoolQueueInfo> namedQueueItr = namedQueuesInfo.values().iterator();
					namedQueueItr.hasNext();) {		
			NamedPoolQueueInfo thisNamedQueueInfo = namedQueueItr.next();				
			addQueueInfo( update, pathToNamedQueues, thisNamedQueueInfo.getName(), thisNamedQueueInfo, lifetime);					
		}		
	}
}
