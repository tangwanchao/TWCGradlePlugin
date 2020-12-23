package me.twc.gradle;

import java.util.Map;

/**
 * @author 唐万超
 */
@SuppressWarnings("WeakerAccess")
public class BuildApkConfig {
    // 360 加固 jar path,配置就使用加固,否则不使用
    private String jarProtectPath;
    // 360 加固账号
    private String account;
    // 360 加固密码
    private String password;
    // app 名字
    private String appName;
    // productFlavor 对应的 app 名字
    private Map<String,String> appNameForProductFlavor;
    // 多个渠道,使用 , 分割
    private String channels;
    // 腾讯 VasDolly jar path,配置了该属性代表使用多渠道打包,否则不使用多渠道打包
    private String jarVasDollyPath;
    // 蒲公英 _api_key
    private String pgyerApiKey;
    // 是否支持 x86
    private boolean supportX86;


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

    /**
     * @return [true : 使用蒲公英内测上传]
     */
    public boolean usePgyer(){
        String apiKey = getPgyerApiKey();
        return apiKey != null && !apiKey.isEmpty();
    }

    /**
     *
     * @param productFlavor productFlavorName
     * @return 输出文件的 apk 名称
     */
    public String getAppNameByProductFlavor(String productFlavor){
        if (productFlavor == null || productFlavor.isEmpty()){
            return getAppName();
        }
        Map<String, String> appNameForProductFlavor = getAppNameForProductFlavor();
        if (appNameForProductFlavor != null && appNameForProductFlavor.containsKey(productFlavor)){
            return appNameForProductFlavor.get(productFlavor);
        }
        return null;
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

    public Map<String, String> getAppNameForProductFlavor() {
        return appNameForProductFlavor;
    }

    public void setAppNameForProductFlavor(Map<String, String> appNameForProductFlavor) {
        this.appNameForProductFlavor = appNameForProductFlavor;
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

    public String getPgyerApiKey() {
        return pgyerApiKey;
    }

    public void setPgyerApiKey(String pgyerApiKey) {
        this.pgyerApiKey = pgyerApiKey;
    }

    public boolean isSupportX86() {
        return supportX86;
    }

    public void setSupportX86(boolean supportX86) {
        this.supportX86 = supportX86;
    }

    @Override
    public String toString() {
        return "BuildApkConfig{" +
                "jarProtectPath='" + jarProtectPath + '\'' +
                ", account='" + account + '\'' +
                ", password='" + password + '\'' +
                ", appName='" + appName + '\'' +
                ", appNameForProductFlavor=" + appNameForProductFlavor +
                ", channels='" + channels + '\'' +
                ", jarVasDollyPath='" + jarVasDollyPath + '\'' +
                ", pgyerApiKey='" + pgyerApiKey + '\'' +
                ", supportX86=" + supportX86 +
                '}';
    }
}
