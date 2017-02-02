# Talos for Android
Talos Framework for Android Applications and/or Smartphone-as-Gateway IoT Applications. 

## Background
For more information about the Talos Project please see the [project website](https://talos-crypto.github.io).
The embedded version of the framework (designed for IoT devices with IP functionality) can be found [here](https://github.com/Talos-crypto/Talos-Contiki)


## Setup Example Applications
We provide three exmaple Android applications, the [Sensor app](Talos-Android/TalosAndroid/TalosModuleApp/), the [Fitbit App](Talos-Android/TalosAndroid/TalosFitbitApp/) and the [Ava Health App](Talos-Android/TalosAndroid/TalosAvaApp/). Before running the applications serveral things have to be considered, which we will outline in the following tutorial.

### Install Talos Cloud and Dependencies
In first step we need to setup the Talos Cloud environment. We recommend using the Ubuntu operating system. You can either use a local machine or a cloud service such as AWS EC2, Azure etc. Make sure you know the IP of your machine (Needed by the Android applications) 
1. First run the Cloud installation script [installTalosCloud.sh](installTalosCloud.sh), which install a glassfish 4 JAVA Application server, the MySQL database and the Talos dependencies. 
2. After a successfull installtion a glassfish 4 deamon should be running and you can access it's admin page on localhost:4848. In a second step, we deploy the Java BE appliactions and the databases for the example applications by running the [installExampleAppsBE.sh](installExampleAppsBE.sh) script.
After sucessfully completing this two steps, a mysql server sould be running containing the example application databases and functions and a glassfish 4 application server instance runs the three backend appliactions. You are ready for deploying the android applications.

### Install a Talos Android Example Application
As mentioned above we provide three example Apllication. In this section we focus on the the [Sensor app](Talos-Android/TalosAndroid/TalosModuleApp/). You need a ARM based Andoird smartphone and a computer with Android Studio installed. 
1. Install the [Android Studio](https://developer.android.com/studio/index.html) IDE.
2. Open the TalosModuleApp Project and download the requerided sdk's. Should be proposed by the ide.
3. Our exmaple applications use the Google-Sign service from Google. In order to use it you have to register your api credentials for each Example App. For this you need a google account (its free). Follow the [steps](https://developers.google.com/mobile/add?platform=android&cntapi=signin&cntapp=Default%20Demo%20App&cntpkg=com.google.samples.quickstart.signin) in the workflow and fill in the missing values. (the app package name is ch.ethz.inf.vs.talsomoduleapp). After the complention you should have a google sign in file.
4. Put the service google-services.json file in the [app](Talos-Android/TalosAndroid/TalosModuleApp/app) directory of the project.
5. Fill in the missing metadata in the [strings.xml](Talos-Android/TalosAndroid/TalosModuleApp/app/src/main/res/values/strings.xml) android resource file. You should provide the IP/DNS address of your Talos Cloud Server and the Google Service API id for the web client (should look like this: blablabla.apps.googleusercontent.com). (!Important the server api key and not the client api key).
6. Also edit the web client id in the BE config file located in /glassfish4/glassfish/domains/domain1/config/app_name.properties on your Talos Cloud Server.
7. Deploy your application on the arm based smartphone. (!Only ARM architecture supported for easy setup, native libaries are precombiled for arm based systems)
The setup procedure is similar for the Fitbit and AvaApp. There is a small exception for the Fitbit app. In order to get the data from your Fitit account you have to [register](https://dev.fitbit.com/) for using the fitbit api. The provided key has to be added to the [strings.xml](Talos-Android/TalosAndroid/TalosFitbitApp/app/src/main/res/values/strings.xml) file.

