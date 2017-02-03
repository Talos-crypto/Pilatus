# Talos for Android
Talos Framework for Android Applications and/or Smartphone-as-Gateway IoT Applications. 

## Background
For more information about the Talos Project please see the [project website](https://talos-crypto.github.io).
The embedded version of the framework (designed for IoT devices with IP functionality) can be found [here](https://github.com/Talos-crypto/Talos-Contiki)

## Structure
This repository contains serveral parts of the talos framwork. The individual parts are partitioned by folders. 
### TalosAndroid
The TalosAndroid folder contains the three example apps the [Sensor App](https://github.com/Talos-crypto/Talos-Android/tree/master/TalosAndroid/TalosModuleApp), the [Fitbit App](https://github.com/Talos-crypto/Talos-Android/tree/master/TalosAndroid/TalosFitbitApp) and the [Ava Health App](https://github.com/Talos-crypto/Talos-Android/tree/master/TalosAndroid/TalosAvaApp). Each of this apps uses the talos andoird library for querying and inserintg encrypted to the cloud. The module can be found [here](https://github.com/Talos-crypto/Talos-Android/tree/master/TalosAndroid/TalosModuleApp/talosmodule). We implemented and optimized the following ciphers for Android (partly native in C and Java):

*optimized additive homomorphic EC-ELGamal

*proxy-re-encryption (PRE) (allows addition and sharing)

*mutable order-preserving-ecnryption mOPE (range queries)

*Paillier (for comparison to EC-ElGamal (native + Java))

### TalosCiphers
This folder contains experimental code, and contains some ciphers that we were evaluating. 

*additive homomorphic EC-ELGamal in C implemented with relic and wrapper for Java+Android

*proxy-re-encryption (PRE) in C implemented with relic and wrapper for Java+Android

*a key homomorphic cipher

*experimental code with ORE, an order preserving scheme

*Pollard Kangoroo in C 

### TalosCloud 
In the TalosCloud folder are the java web application and the MySQL user defined functions (UDF's). The Java application implements the TalosCloud logic and provides a REST api for the android libary. The UDF's are used for the aggreagation and operations over the encrypted data. 

### TalosHelper
Various helper and instalöation scripts are located in the TalosHelper folder. 


## Setup Example Applications
We provide three example Android applications, the [Sensor App](https://github.com/Talos-crypto/Talos-Android/tree/master/TalosAndroid/TalosModuleApp), the [Fitbit App](https://github.com/Talos-crypto/Talos-Android/tree/master/TalosAndroid/TalosFitbitApp) and the [Ava Health App](https://github.com/Talos-crypto/Talos-Android/tree/master/TalosAndroid/TalosAvaApp). Before running the applications several things have to be considered, which we will outline in the following tutorial.

### Install Talos Cloud and Dependencies
In a first step, we need to setup the Talos Cloud environment. We recommend using the Ubuntu operating system. You can either use a local machine or a cloud service such as AWS EC2, Azure etc. Make sure you know the IP of your machine (Needed by the Android applications) 

1. First run the Cloud installation script [installTalosCloud.sh](installTalosCloud.sh), which install a glassfish 4 JAVA Application server, the MySQL database, and the Talos dependencies. 

2. After a successful installation, a glassfish 4 deamon should be running and you can access it's admin page on localhost:4848. In a second step, we deploy the Java BE applications and the databases for the example applications by running the [installExampleAppsBE.sh](installExampleAppsBE.sh) script.

After successfully completing this two steps, a MySQL server should be running containing the example application databases and functions and a glassfish 4 application server instance runs the three backend applications. You are ready for deploying the android applications.

### Install a Talos Android Example Application
As mentioned above we provide three example Application. In this section, we focus on the [Sensor App](https://github.com/Talos-crypto/Talos-Android/tree/master/TalosAndroid/TalosModuleApp). You need an ARM-based Android smartphone and a computer with Android Studio installed.

1. Install the [Android Studio](https://developer.android.com/studio/index.html) IDE.

2. Open the TalosModuleApp Project and download the required sdks. Should be proposed by the IDE.

3. Our example applications use the Google-Sign service from Google. In order to use it, you have to register your API credentials for each Example App. For this, you need a google account (its free). Follow the [steps](https://developers.google.com/mobile/add?platform=android&cntapi=signin&cntapp=Default%20Demo%20App&cntpkg=com.google.samples.quickstart.signin) in the workflow and fill in the missing values. (the app package name is ch.ethz.inf.vs.talsomoduleapp). After the completion, you should have a google sign in file.

4. Put the service google-services.json file in the [app](Talos-Android/TalosAndroid/TalosModuleApp/app) directory of the project.

5. Fill in the missing metadata in the [strings.xml](https://github.com/Talos-crypto/Talos-Android/blob/master/TalosAndroid/TalosModuleApp/app/src/main/res/values/strings.xml) android resource file. You should provide the IP/DNS address of your Talos Cloud Server and the Google Service API id for the web client (should look like this: blablabla.apps.googleusercontent.com). (!Important the server API key and not the client API key).

6. Also, edit the web client id in the BE config file located in /glassfish4/glassfish/domains/domain1/config/app_name.properties on your Talos Cloud Server.

7. Deploy your application on the arm based smartphone. (!Only ARM architecture supported for easy setup, native libraries are precompiled for arm based systems)

The setup procedure is similar for the Fitbit and AvaApp. There is a small exception for the Fitbit app. In order to get the data from your Fitbit account, you have to [register](https://dev.fitbit.com/) for using the Fitbit API. The provided key has to be added to the [strings.xml](https://github.com/Talos-crypto/Talos-Android/blob/master/TalosAndroid/TalosFitbitApp/app/src/main/res/values/strings.xml) file.

