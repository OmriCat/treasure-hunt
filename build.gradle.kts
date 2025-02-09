plugins {
  plugins { id("com.osacky.doctor") version "0.10.0" }
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.compose) apply false
}
