package com.matburt.mobileorg;
import android.content.res.Resources.NotFoundException;

public interface Synchronizer
{
    public void pull() throws NotFoundException, ReportableError;
    public void push() throws NotFoundException, ReportableError;
    public void close();
}
