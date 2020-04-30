package me.twc.gradle;

/**
 * @author 唐万超
 * @date 2020/04/28
 */
public class BuildApkConfig {
    // 360 加固 jar path,配置就使用加固,否则不使用
    private String jarProtectPath;
    // 360 加固账号
    private String account;
    // 360 加固密码
    private String password;
    // APP名字
    private String appName;
    // 多个渠道,使用 , 分割
    private String channels;
    // 腾讯 VasDolly jar path,配置了该属性代表使用多渠道打包,否则不使用多渠道打包
    private String jarVasDollyPath;


    /**
     * @return [true : 使用加固]
     *         [false : 不使用加固]
     */
    public boolean use360Protect() {
        return getJarProtectPath() != null &&
                getAccount() != null &&
                getPassword() != null;
    }

    /**
     *
     * @return [true : 使用多渠道打包]
     *         [false : 不使用多渠道打包]
     */
    public boolean useChannels(){
        return getJarVasDollyPath() != null &&
                getChannels() != null;
    }

    @Override
    public String toString() {
        return "BuildApkConfig{" +
                "jarProtectPath='" + jarProtectPath + '\'' +
                ", account='" + account + '\'' +
                ", password='" + password + '\'' +
                ", appName='" + appName + '\'' +
                ", channels='" + channels + '\'' +
                ", jarVasDollyPath='" + jarVasDollyPath + '\'' +
                '}';
    }

    public String getJarProtectPath() {
        return jarProtectPath;
    }

    public void setJarProtectPath(String jarProtectPath) {
        this.jarProtectPath = jarProtectPath;
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

    public String getChannels() {
        return channels;
    }

    public void setChannels(String channels) {
        this.channels = channels;
    }

    public String getJarVasDollyPath() {
        return jarVasDollyPath;
    }

    public void setJarVasDollyPath(String jarVasDollyPath) {
        this.jarVasDollyPath = jarVasDollyPath;
    }
}
