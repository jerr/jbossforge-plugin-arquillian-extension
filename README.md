# jbossforge-plugin-arquillian-extension

**Forge Arquillian Extension Plugin** was created to help you writing tests with arquillian extensions.

# Installation
 
The Arquillian Extension plugin is listed in the Forge plugin repository so installation is trivial. 
In Forge type: 

	forge install-plugin arquillian-extension

That's it! The plugin will be downloaded from the git repository, compiled and installed.

# Drone Extension Commands
Don't forget to configure Arquillian plugin before using this extension plugin. (arquillian setup)
* `arquillian-drone setup` - Instals Arquillian Drone dependency.
* `arquillian-drone configure-webdriver` - Configure extension with the webdriver qualifier in the arquillian.xml file.
	+ `--browserCapabilities` : Determines which browser instance is created for WebDriver testing. Default value is htmlUnit.
	+ `--remoteAddress` : Default address for remote driver to connect. Default value is http://localhost:14444/wd/hub
	+ `--chromeDriverBinary` : Path to chromedriver binary.
	+ `--firefoxExtensions` : Path or multiple paths to xpi files that will be installed into Firefox instance as extensions. Separate paths using space, use quotes in case that path contains spaces.
* `arquillian-drone create-test` - Create a new test class with a default @Deployment method and @Drone WebDriver attribute. 
	+ `--package` : the test class package
	+ `--named` : the test class name
	
# Graphene Extension Commands
Don't forget to configure arquillian-drone plugin before using this extension plugin. (arquillian-drone setup)
* `arquillian-graphene setup` - Instals Arquillian Graphene dependency.
* `arquillian-graphene new-page` - Create a new graphene page class
	+ `--package` : the page class package
	+ `--named` : the page class name
* `arquillian-graphene new-element` - Create a new WebElement in the current class
	+ `--named` : the WebElement attribute name
	+ `--findby` : the locator type (ex: id )
	+ `--value` : the locator value (ex: myid )
	
## Simple demo

	forge git-plugin git://github.com/jerr/jbossforge-plugin-arquillian-extension.git
	new-project --named demo --type war 
	servlet setup --quickstart 
	build
	as7 setup
	as7 start
	
Configure arquillian with JBoss AS **7.1.1** container : (Be careful to choose the correct JBoss version)	
	
	arquillian setup --containerType REMOTE --containerName JBOSS_AS_REMOTE_7.X 
	
	arquillian-drone setup 
	arquillian-graphene setup 
	arquillian-drone create-test --named Index
	arquillian-graphene new-page --named Index
	arquillian-graphene new-element --named documentationLink --findby linkText --value Documentation
	cd ..	
	edit IndexTest.java
Add the page in the test:
```java
   @Page
   private IndexPage indexPage;
```
Edit the  test method to use the page:
```java
   @Test
   public void testIsDeployed()
   {
      browser.navigate().to(baseUrl);
      org.junit.Assert.assertTrue(indexPage.getDocumentationLink().isDisplayed());
   }
```

Test the project :
	
	build --profile arq-jboss_as_remote_7.x 
	as7 shutdown

# Arquillian Droidium Web Commands
Don't forget to configure Arquillian plugin before using this extension plugin. (arquillian setup)
* `arquillian-droidium-web setup` - Installs Droidium web plugin
* `arquillian-droidium create-test` - Creates simple Droidium web test
* `arquillian-droidium configure-droidium-web` - Configures Droidium web plugin

# Arquillian Droidium Native Commands
Don't forget to configure Arquillian plugin before using this extension plugin. (arquillian setup)
* `arquillian-droidium-native setup` - Installs Droidium native plugin
* `arquillian-droidium create-test` - Creates simple Droidium native test
* `arquillian-droidium configure-droidium-native` - Configures Droidium native plugin

# Performance Extension Commands
# Persistence Extension Commands
# Portal Extension Commands
# Warp Extension Commands
Don't forget to configure arquillian-drone plugin before using this extension plugin. (arquillian-drone setup)
* `arquillian-warp setup` - Instals Arquillian Warp dependency.
