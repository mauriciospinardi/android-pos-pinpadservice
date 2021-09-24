# Pinpad Service

Initially built as a (P)roof (O)f (C)oncept, the Pinpad Service simulates an
ABECS PINPAD for POS solutions.  
From the entire ABECS specification, multimedia, proprietary and deprecated
instructions are left out, reducing scope to 18 instructions:  

- OPN, GIN and CLX
- CEX, CHP, EBX, GCD, GPN, GTK, MNU and RMC
- TLI, TLR and TLE
- GCX, GED, GOX and FCX

Be sure to check [ABECS](https://www.abecs.org.br/) website and
[spec.](https://www.abecs.org.br/certificacao-funcional-dos-pinpads) to have a
deeper understanding on how to make requests and handle its responses.  

Available in its own installable package, the Pinpad Service can be updated
independently from other applications and shared by them, reducing costs of OTA
data usage and code maintenance.  
The Library that pairs with it helps consumers to hide the complexity of
binding the service, as well as handling AIDL restrictions. It serves as
handler and it was designed to avoid direct connections.  

Be sure to check the [DEMO application](#demo-application) for an example on
how to consume the Pinpad Service using the associated Pinpad Library.

## Local dependencies

Local dependencies are those which are _private_, but within the scope of the
Pinpad Service development team. They need to be made available locally before
the Pinpad Service can be built:  

1. Clone the repository [android-misc-loglibrary](https://github.com/mauriciospinardi-cloudwalk/android-misc-loglibrary)
2. Rebuild the `release` variant based on a tag of your choice.
3. Run task: `gradle publishToMavenLocal`

Repeat the above steps to all required local dependencies and you're ready! To
know which are the required ones, check the `build.gradle` file of each Pinpad
Service module.

## Copyrighted dependencies

Copyrighted dependencies are those provided by third parties. They are under
NDA and can't be made public.  
For each platform integrated to the Pinpad Service, reach out the respective
platform representatives.  

## DEMO application

The Pinpad Service includes a DEMO module, covering the very basics:  

- [SplashActivity.java](DEMO/src/main/java/io/cloudwalk/pos/demo/presentation/SplashActivity.java)
  shows how to connect with the service using the Pinpad Library.
- [MainActivity.java](DEMO/src/main/java/io/cloudwalk/pos/demo/presentation/MainActivity.java)
  shows how to perform local requests.
  - _Check [DEMO.java](DEMO/src/main/java/io/cloudwalk/pos/demo/DEMO.java) for
    samples of requests made through the `Bundle` interface_.

An ABECS PINPAD natively exchanges `byte[]` streams. However, the Pinpad
Service allows local requests made through a `Bundle` interface, for easier
handling. The Pinpad Library is the one responsible for the conversion between
`Bundle` and `byte[]` interfaces. In any case, if one decides to use the native
`byte[]` interface, it can do so:

- _Bundle_ API
  - `PinpadManager#abort();`
  - `PinpadManager#request(IServiceCallback, Bundle);`
- _byte[]_ API
  - `PinpadManager#send(IServiceCallback, byte[], int);`
  - `PinpadManager#receive(byte[], long);`

<!-- TODO: summary of the `Bundle` API characteristics -->
