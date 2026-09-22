package eu.europa.ec.simpl.usersroles.controllers;

import eu.europa.ec.simpl.api.applicationinfo.v1.exchanges.ApplicationInfoApi;
import eu.europa.ec.simpl.api.applicationinfo.v1.model.ApplicationVersionDTO;
import eu.europa.ec.simpl.common.services.ApplicationInfoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1")
public class ApplicationInfoController implements ApplicationInfoApi {

    private final ApplicationInfoService service;

    public ApplicationInfoController(ApplicationInfoService service) {
        this.service = service;
    }

    @Override
    public ApplicationVersionDTO version() {
        return service.version();
    }
}
