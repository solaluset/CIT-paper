-keep class org.vinerdream.**
-keepclassmembers class org.vinerdream.CitCliMain {
    public static void main(java.lang.String[]);
}
# Keep all enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class * {
    @org.bukkit.event.EventHandler *;
}
-keepclassmembers class * extends org.bukkit.event.Event {
    public ** *(...);
}
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations
-dontwarn org.vinerdream.citPaper.libs.**
-dontwarn me.gabytm.util.**
