# Spring Boot Apache Fortress Sample

Using Apache Fortress RBAC for Spring Boot app authentication and authorization

This sample code uses minimal Spring Boot application created using Spring Initializr with:
 
    spring-boot-starter-web
    spring-boot-starter-security

### Steps to add Apache Fortress as authentication provider 

1. Add dependencies

		<dependency>
			<groupId>org.apache.directory.fortress</groupId>
			<artifactId>fortress-core</artifactId>
			<version>2.0.5</version>
			<exclusions>
				<exclusion>
					<groupId>org.slf4j</groupId>
					<artifactId>slf4j-log4j12</artifactId>
				</exclusion>
			</exclusions>
		</dependency>

2. Add **ehcache.xml** and **fortress.properties** to app _resources_ directory

3. Edit fortress.properties to specify your LDAP server host address. Note that LDAP server must be prepared for Apache Fortress. See Apache Fortress installation docs for more info.

4. Add FortressSecurityConfigurerAdapter class in your application base namespace. It will automatically plug-in into Spring Security.

 File: _FortressSecurityConfigurerAdapter.java_
    
    package com.example.apachefortressdemo;
    
    import org.apache.directory.fortress.core.AccessMgr;
    import org.apache.directory.fortress.core.AccessMgrFactory;
    import org.apache.directory.fortress.core.SecurityException;
    import org.apache.directory.fortress.core.model.Session;
    import org.apache.directory.fortress.core.model.User;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
    import org.springframework.security.authentication.AuthenticationProvider;
    import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
    import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
    import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
    import org.springframework.security.core.Authentication;
    import org.springframework.security.core.AuthenticationException;
    import org.springframework.security.core.GrantedAuthority;
    import org.springframework.security.core.authority.SimpleGrantedAuthority;
    
    import java.util.Collection;
    import java.util.stream.Collectors;

    @Configuration
    public class FortressSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
        @Bean
        public AccessMgr getAccessMgr() throws SecurityException {return AccessMgrFactory.createInstance();	}
    
        @Autowired
        AccessMgr accessMgr;
    
        @Override
        protected void configure(AuthenticationManagerBuilder auth) {
            auth.authenticationProvider(new AuthenticationProvider() { //Fortress authentication provider
                @Override
                public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                    String name = authentication.getName();
                    String password = authentication.getCredentials().toString();
    
                    // use the credentials and authenticate against Apache Fortress system
                    try {
                        Session session = accessMgr.createSession(new User(name,password),false);
                        return new Authentication() {
                            @Override
                            public Collection<? extends GrantedAuthority> getAuthorities() {
                                //role is a GrantedAuthority that starts with the prefix ROLE_. this is to enable use of "hasRole" API
                                return  session.getRoles().stream().map(x -> new SimpleGrantedAuthority("ROLE_" + x.getName())).collect(Collectors.toList());
                            }
    
                            @Override
                            public Object getCredentials() {
                                return null;
                            }
    
                            @Override
                            public Object getDetails() {
                                return session;
                            }
    
                            @Override
                            public Object getPrincipal() {
                                return session.getUser();
                            }
    
                            @Override
                            public boolean isAuthenticated() {
                                return session.isAuthenticated();
                            }
    
                            @Override
                            public void setAuthenticated(boolean b) throws IllegalArgumentException {
                                session.setAuthenticated(b);
                            }
    
                            @Override
                            public String getName() {
                                return session.getUser().getName();
                            }
                        };
                    } catch (SecurityException e) {
                        e.printStackTrace();
                        throw new AuthenticationCredentialsNotFoundException(e.getMessage());
                    }
                }
    
                @Override
                public boolean supports(Class<?> authentication) {
                    return authentication.equals(UsernamePasswordAuthenticationToken.class);
                }
            });
        }
    
    }
 
 Now, you will see a standard Spring login screen, but it will be validated against Apache Fortress LDAP.
 
 ### Using Fortress in your application
 
 Spring authorization annotations like @Secured, @RolesAllowed, @PreAuthorize, @PostAuthorize, etc now work with user roles received from Fortress.
 
     @RolesAllowed({ "ROLE_VIEWER", "ROLE_EDITOR" })
     public boolean securedMethod(String username) {
         //...
     }
     
     @PreAuthorize("hasRole('ROLE_VIEWER')")
     public String onlyViewers() {
         //...
     }
 
 To access Fortress User object of currently logged-in user, access Spring Authentication object and get Principal property:
 
     @RequestMapping("/profile")
     public String actionHome(Model ui, Authentication auth){
         User currentUser= (User) auth.getPrincipal();
         ...

 Also, current Fortress Session is accessible by Details property:
 
         Session currentSession= (Session) auth.getDetails();
         ...
         
 ### Checking Fortress permissions

    @Autowired
    AccessMgr accessMgr;
    
    public void someMethod()
    { 
        Session currentSession=(Session)auth.getDetails();
        String objectName="/something";
        String operationName="READ";
        boolean isPermitted = accessMgr.checkAccess(currentSession, new Permission(objectName, operationName));
        ...
 
 ### Running this sample
 
 Before running this sample application make sure you adjust values in fortress.properties file. 
 
    mvn spring-boot:run

 Then, go to http://localhost:8080 .