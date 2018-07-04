# 学习记录

# 任务列表

## routing


# 遇到的问题
## access denied
```
2018-06-06 10:09:42,478 main ERROR Could not register mbeans java.security.AccessControlException: access denied ("javax.management.MBeanTrustPermission" "register")
	at java.base/java.security.AccessControlContext.checkPermission(AccessControlContext.java:472)
	at java.base/java.lang.SecurityManager.checkPermission(SecurityManager.java:371)
	at java.management/com.sun.jmx.interceptor.DefaultMBeanServerInterceptor.checkMBeanTrustPermission(DefaultMBeanServerInterceptor.java:1805)
	at java.management/com.sun.jmx.interceptor.DefaultMBeanServerInterceptor.registerMBean(DefaultMBeanServerInterceptor.java:318)
	at java.management/com.sun.jmx.mbeanserver.JmxMBeanServer.registerMBean(JmxMBeanServer.java:522)
	at org.apache.logging.log4j.core.jmx.Server.register(Server.java:389)
	at org.apache.logging.log4j.core.jmx.Server.reregisterMBeansAfterReconfigure(Server.java:167)
	at org.apache.logging.log4j.core.jmx.Server.reregisterMBeansAfterReconfigure(Server.java:140)
	at org.apache.logging.log4j.core.LoggerContext.setConfiguration(LoggerContext.java:556)
	at org.apache.logging.log4j.core.LoggerContext.start(LoggerContext.java:261)
	at org.apache.logging.log4j.core.impl.Log4jContextFactory.getContext(Log4jContextFactory.java:206)
	at org.apache.logging.log4j.core.config.Configurator.initialize(Configurator.java:220)
	at org.apache.logging.log4j.core.config.Configurator.initialize(Configurator.java:197)
	at org.elasticsearch.common.logging.LogConfigurator.configureStatusLogger(LogConfigurator.java:171)
	at org.elasticsearch.common.logging.LogConfigurator.configure(LogConfigurator.java:140)
	at org.elasticsearch.common.logging.LogConfigurator.configure(LogConfigurator.java:119)
	at org.elasticsearch.bootstrap.Bootstrap.init(Bootstrap.java:291)
	
```

## SecurityManager

```text
System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPermission(Permission perm) {
                // grant all permissions so that we can later set the security manager to the one that we want
            }
        });
```
## org.elasticsearch.plugins.PluginsService
PluginsService.sortBundles -- 从modules文件夹中读取插件信息，及所有的jar文件；
遍历插件，同时递归遍历extended.plugins属性指定的其它插件
已经遍历过的插件不再遍历
循环依赖时报错
plugin-descriptor.properties => extended.plugins => 对应其它的plugin



# 如何debug es
参考项目根目录文件，TESTING.asciidoc
