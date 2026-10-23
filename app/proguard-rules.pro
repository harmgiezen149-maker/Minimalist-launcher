# Everything the system starts by name (activities, services, the
# application class) is declared in the manifest, and R8 keeps those
# automatically. The app uses no reflection or serialization libraries, so
# no extra keep rules are needed.

# Readable stack traces in the local crash log: keep file names and line
# numbers, but not the original identifiers.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
