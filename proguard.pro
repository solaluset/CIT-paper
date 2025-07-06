-keep class org.vinerdream.**
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
