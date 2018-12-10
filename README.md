# Microclimate Developer Tools for Eclipse

## How to install

Complete the following steps to install Microclimate Developer Tools for Eclipse:

1. Download and install the latest [Eclipse IDE for Java EE Developers](https://www.eclipse.org/downloads/packages/release/) or use an existing installation. The earliest supported version of the Eclipse IDE for Microclimate Developer Tools for Eclipse is 4.8 (Photon).
2. Install the [Microclimate Developer Tools from Eclipse Marketplace](https://marketplace.eclipse.org/content/microclimate-developer-tools-beta).

## How to use

- Create a new connection to Microclimate:
    - From the **File** menu select **New** > **Other** > **Microclimate** > **New Microclimate Connection**
    - Fill in the host (only **localhost** is supported) and port (usually **9090**)
    - Click the **Test Connection** and then click **Finish**
- This should open the **Microclimate Explorer** view with the connection expanded to show your projects. If it does not open the view then open it manually:
    - From the **Window** menu select **Show View** > **Other** > **Microclimate** > **Microclimate Explorer**
    - Click **Open**
- Right click on the connection to see the available actions for connections
- Right click on a project to see the available actions for projects

## Features

- View all Microclimate projects for a connection including the application and build status
- Import your Microclimate project into the Eclipse workspace
- Debug **Microprofile/Java EE** and **Spring** applications
- View application and build logs in the **Console** view
- Open your application in the default Eclipse browser
- Integrate Microclimate validation errors into the **Markers** view
- Open a shell session into a Microclimate application container
- Open the application monitor page
- Toggle the project auto build setting
- Manually initiate project builds

(Add documentation link)

## Contributing

We welcome issues (make this a link) and contributions from the public! See [CONTRIBUTING.md](https://github.ibm.com/dev-ex/microclimate-vscode/tree/master/CONTRIBUTING.md)





