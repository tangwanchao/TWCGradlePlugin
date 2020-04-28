package me.twc.gradle;

/**
 * @author 唐万超
 * @date 2020/04/28
 */
public class BuildApkConfig {
    private String jarPath;
    private String account;
    private String password;
    private String appName;
    private String channelFilePath;


    public boolean check() {
        return getJarPath() != null &&
                getAccount() != null &&
                getPassword() != null;
    }

    @Override
    public String toString() {
        return "BuildApkConfig{" +
                "jarPath='" + jarPath + '\'' +
                ", account='" + account + '\'' +
                ", password='" + password + '\'' +
                ", appName='" + appName + '\'' +
                ", channels=" + channelFilePath +
                '}';
    }

    public String getJarPath() {
        return jarPath;
    }

    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getChannelFilePath() {
        return channelFilePath;
    }

    public void setChannelFilePath(String channelFilePath) {
        this.channelFilePath = channelFilePath;
    }
}
