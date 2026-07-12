# Car App Library (Android Auto) — le service et les écrans sont découverts par
# l'hôte via le manifeste et instanciés dynamiquement : R8 ne doit ni les
# renommer ni les supprimer, sinon l'app n'apparaît pas / plante à l'ouverture.
-keep public class * extends androidx.car.app.CarAppService { *; }
-keep public class * extends androidx.car.app.Session { *; }
-keep public class * extends androidx.car.app.Screen { *; }
-keep class androidx.car.app.** { *; }
-dontwarn androidx.car.app.**

# Sécurité : conserve nos points d'entrée Android Auto nommés dans le manifeste.
-keep class com.mkmemories.copilot.car.** { *; }
