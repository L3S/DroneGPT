
# DroneGPT

DroneGPT is an Android application that allows ChatGPT to control DJI Aircrafts. 

The app sends ChatGPT a prompt that contains the available API library, which is used by ChatGPT to develop the flight algorithm in [Lua programming language](https://www.lua.org/). The high-level Lua API functions execute their respective [DJI MSDK v5](https://github.com/dji-sdk/Mobile-SDK-Android-V5) API functions at run-time.


[![MIT License](https://img.shields.io/badge/License-MIT-green.svg)](https://choosealicense.com/licenses/mit/)


## Installation

### Prerequisites

- Android Studio: `Android Studio Giraffe 2022.3.1`.
- A DJI drone compatible with [DJI MSDK V5](https://developer.dji.com/mobile-sdk/) along with its remote controller. 
- Anroid device: DJI MSDK officially supports devices running Android 6.0 or higher. This project was tested on Samsung Galaxy A21s running Android 12.

- optional: [Android bridge app](https://github.com/dji-sdk/Android-Bridge-App).

> [!NOTE]
> This project was tested using DJI Mini 3 with DJI RC N1. The remote controller with the built-in screen is not compatible with DJI MSDK!

### Environment Variables

To run this project, you will need to add the following environment variables in [the gradle.properties file](https://github.com/L3S/DroneGPT/blob/dev-sdk-main/SampleCode-V5/android-sdk-v5-as/gradle.properties).

- `DJI_MSDK_API_KEY`: can be obtained for free by creating a [DJI developer account and applying for a key](https://developer.dji.com/user/apps/#all). **Please note** that you have to enter "com.dji.sampleV5.aircraft" as "Package name" when creating the key.

- `OPENAI_API_KEY`: can be created in OpenAI's [API Keys dashboard](https://platform.openai.com/api-keys).



### Build
1. From Android Studio sync gradle project.
2. Pair your Android device over wifi.
3. Click "Run" on Android Studio
4. Connect the Android device with the DJI Remote Controller
    


> [!IMPORTANT]  
> After building the project on your Android device, test the DJI registration by making sure it says "Registered" under "Registeration Status" on the app's homescreen.


## Usage

Since this app was developed for a research paper, flights are conducted by creating surveillance "experiments" and defining the allowed flight area for ChatGPT.
To mitigate safety risks, ChatGPT's control of the drone is limited to horizontal movements. Therefore, takeoff, landing and vertical throlttling commands are excluded from ChatGPT's API.

> [!TIP]
> Before proceeding with the steps below for the first time, test the drone's connectivity by conducting a manual flight in the "Default Layout" from the app's home screen.

  1. Create experiment: choose the OpenAI model, description of the allowed area and the height of the flight.
  2. Select the created experiment from the scrollable left side bar
  3. Switch to ChatGPT's interface: the "Show flight logs" radio button is used to toggle between the display of flight logs and ChatGPT's interface
  4. Generate and send prompt: select the small rounded button left to the input field. The input field can be optionally edited before sending the final prompt.
  5. After iterating on ChatGPT's implementations, the experiment can be initialized by clicking the button to activate safety functions (Init All) to take off and subsequently increase the drone’s altitude based on the saved height of the selected experiment (Elevate to experiment height)
  6. After the drone hovers at the desired altitude, execute ChatGPT's code by long-clicking the response, reviewing the parsed code, and selecting "Execute"




## Acknowledgements

 - [DJI MSDK and Sample app](https://github.com/dji-sdk/Mobile-SDK-Android-V5)
 - [ChatGPT Chat UI](https://github.com/nohjunh/ChatGPTAndroid)


