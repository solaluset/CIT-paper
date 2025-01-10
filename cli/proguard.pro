-keep class org.vinerdream.**
-keepclassmembers class org.vinerdream.CitCliMain {
    public static void main(java.lang.String[]);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-dontwarn kotlin.**
-dontwarn com.google.**
-dontwarn org.eclipse.**
-dontwarn org.osgi.**
-dontwarn org.apache.maven.repository.**
-dontwarn io.papermc.paper.event.executor.**
