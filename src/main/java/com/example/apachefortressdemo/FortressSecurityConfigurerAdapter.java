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
