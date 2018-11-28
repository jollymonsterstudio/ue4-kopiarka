/*
 * Copyright 2018. Jolly Monster Studio ( jollymonsterstudio.com )
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jollymonsterstudio.unreal.handler;

import com.jollymonsterstudio.unreal.service.CopyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * StartupHandler - the only thing this does is kick off the copy process during application startup
 */
@Component
public class StartupHandler {

    private CopyService copyService;

    public StartupHandler(@Autowired final CopyService copyService) {
        this.copyService = copyService;
    }

    @PostConstruct
    public void init(){
        copyService.copy();
    }
}