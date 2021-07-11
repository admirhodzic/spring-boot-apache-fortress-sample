package com.example.apachefortressdemo;

import org.apache.directory.fortress.core.AccessMgr;
import org.apache.directory.fortress.core.SecurityException;
import org.apache.directory.fortress.core.model.Permission;
import org.apache.directory.fortress.core.model.Session;
import org.apache.directory.fortress.core.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = {"","/"})
public class HomeController {

    @Autowired
    AccessMgr accessMgr;

    @RequestMapping("")
    public String actionHome(Model ui, Authentication auth) throws SecurityException {
        /*
        User currentUser= (User) auth.getPrincipal();

        Session currentSession=(Session)auth.getDetails();

        String objectName="/something";
        String operationName="READ";
        boolean isPermitted = accessMgr.checkAccess(currentSession, new Permission(objectName, operationName));
        */
        ui.addAttribute("user", auth.getName());
        ui.addAttribute("principal",auth.getPrincipal());
        ui.addAttribute("userRoles", auth.getAuthorities());
        return "home";
    }

}
