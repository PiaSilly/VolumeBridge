// IVolumeService.aidl
package com.volumebridge.app;

interface IVolumeService {
    boolean setAppVolume(String packageName, float volume);
    void destroy();
}
