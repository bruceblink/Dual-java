# 一个用Java的processing库写的小游戏

## 游戏界面

![初始界面](picture/pic1)

![游戏界面](picture/pic2.gif)

## 环境要求

- JDK 21
- Gradle（使用内置 Wrapper，无需单独安装）

> `gradle.properties` 中 `org.gradle.java.installations.paths` 写的是本机 JDK 21 的安装路径，
> 换台机器时请修改为实际路径，或将该行删除并把 `JAVA_HOME` 环境变量指向 JDK 21。

## 运行方式

使用Gradle打包成jar，然后运行jar包

```cmd
cd Dual-java
./gradlew run
```
