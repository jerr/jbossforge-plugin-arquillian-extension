# jbossforge-plugin-arquillian-extension

**Forge Arquillian Extension Plugin** was created to help you writing tests with arquillian extensions.

# Installation
 
The Arquillian Extension plugin is listed in the Forge plugin repository so installation is trivial. 
In Forge type: 

	forge install-plugin arquillian-extension

That's it! The plugin will be downloaded from the git repository, compiled and installed.

# Drone Extension Commands
Don't forget to configure Arquillian plugin before using this extension plugin. (arquillian setup)
* `arq-drone setup` - Instals Arquillian Drone dependency.
* `arq-drone configure-webdriver` - Configure extension with the webdriver qualifier in the arquillian.xml file.
	+ `--browserCapabilities` : Determines which browser instance is created for WebDriver testing. Default value is htmlUnit.
	+ `--remoteAddress` : Default address for remote driver to connect. Default value is http://localhost:14444/wd/hub
	+ `--chromeDriverBinary` : Path to chromedriver binary.
	+ `--firefoxExtensions` : Path or multiple paths to xpi files that will be installed into Firefox instance as extensions. Separate paths using space, use quotes in case that path contains spaces.
* `arq-drone create-test` - Create a new test class with a default @Deployment method and @Drone WebDriver attribute. 
	+ `--package` : the test class package
	+ `--named` : the test class name
	
# Graphene Extension Commands
Don't forget to configure arq-drone plugin before using this extension plugin. (arq-drone setup)
* `arq-graphene setup` - Instals Arquillian Graphene dependency.
* `arq-graphene new-page` - Create a new graphene page class
	+ `--package` : the page class package
	+ `--named` : the page class name
* `arq-graphene new-element` - Create a new WebElement in the current class
	+ `--named` : the WebElement attribute name
	+ `--findby` : the locator type (ex: id )
	+ `--value` : the locator value (ex: myid )
	
# Simple demo

	forge git-plugin git://github.com/jerr/jbossforge-plugin-arquillian-extension.git
	new-project --named demo --type war 
	servlet setup --quickstart 
	build
	as7 setup
	as7 start
	
Configure arquillian with JBoss AS **7.1.1** container : (Be careful to choose the correct JBoss version)	
	
	arquillian setup --containerType REMOTE --containerName JBOSS_AS_REMOTE_7.X 
	
	arq-drone setup 
	arq-graphene setup 
	arq-drone create-test --named Index
	arq-graphene new-page --named Index
	arq-graphene new-element --named documentationLink --findby linkText --value Documentation
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
	