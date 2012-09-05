package org.dcache.pinmanager;

import java.util.Date;
import java.util.EnumSet;

import diskCacheV111.util.PnfsId;
import diskCacheV111.vehicles.Message;
import org.dcache.vehicles.FileAttributes;
import org.dcache.namespace.FileAttribute;
import static org.dcache.namespace.FileAttribute.*;
import static com.google.common.base.Preconditions.*;

public class PinManagerExtendPinMessage extends Message
{
    private static final long serialVersionUID = 4239204053634579521L;

    private FileAttributes _fileAttributes;
    private long _pinId;
    private long _lifetime;
    private Date _expirationTime;

    public PinManagerExtendPinMessage(FileAttributes fileAttributes, long pinId, long lifetime)
    {
        checkNotNull(fileAttributes);
        _fileAttributes = fileAttributes;
        _pinId = pinId;
        _lifetime = lifetime;
    }

    public long getPinId()
    {
        return _pinId;
    }

    public PnfsId getPnfsId()
    {
        return _fileAttributes.getPnfsId();
    }

    public FileAttributes getFileAttributes()
    {
        return _fileAttributes;
    }

    public void setLifetime(long lifetime)
    {
        _lifetime = lifetime;
    }

    public long getLifetime()
    {
        return _lifetime;
    }

    public void setExpirationTime(Date expirationTime)
    {
        _expirationTime = expirationTime;
    }

    public Date getExpirationTime()
    {
        return _expirationTime;
    }

    @Override
    public String toString()

    {
        return "PinManagerExtendPinMessage[" + _pinId + "," + _fileAttributes + "," + _lifetime + "]";
    }

    public static EnumSet<FileAttribute> getRequiredAttributes()
    {
        return EnumSet.of(PNFSID);
    }
}
