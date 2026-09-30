# Gnix keeps models free of reflection-based serialization; field names are explicit JSON keys.
-keepclassmembers class com.gnix.app.Article { public <fields>; public <init>(...); }
-keepclassmembers class com.gnix.app.Source { public <fields>; public <init>(...); }
