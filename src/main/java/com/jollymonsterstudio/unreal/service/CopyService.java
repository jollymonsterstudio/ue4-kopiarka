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

package com.jollymonsterstudio.unreal.service;

import com.jollymonsterstudio.unreal.visitor.UnrealFileVisitor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class CopyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CopyService.class);

    @Value("${project.source.directory}")
    private String sourceDir;

    @Value("${project.source.name}")
    private String sourceName;

    @Value("${project.target.directory}")
    private String targetDir;

    @Value("${project.target.name}")
    private String targetName;

    @Value("${config.force.delete}")
    private boolean forceDelete = false;

    @Value("${whitelist.directories}")
    private String[] whitelistDirs;
    @Value("${whitelist.extension.binary}")
    private String[] whitelistBinary;
    @Value("${whitelist.extension.ascii}")
    private String[] whitelistAscii;

    private UnrealFileVisitor fileVisitor;
    private ApplicationContext applicationContext;

    public CopyService(@Autowired final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void copy() {
        LOGGER.info("------------");
        LOGGER.info("  STARTED   ");
        LOGGER.info("------------");

        try {
            Assert.isTrue(StringUtils.isNotEmpty(sourceDir), "Source directory cannot be empty");
            Assert.isTrue(StringUtils.isNotEmpty(sourceName), "Source project name cannot be empty");

            Assert.isTrue(new File(sourceDir).exists(), "Source directory does not exist, check your folder configuration");

            Assert.isTrue(StringUtils.isNotEmpty(targetDir), "Target directory cannot be empty");
            Assert.isTrue(StringUtils.isNotEmpty(targetName), "Source directory cannot be empty");

            fileVisitor = new UnrealFileVisitor(Paths.get(sourceDir), sourceName, Paths.get(targetDir), targetName, whitelistDirs, whitelistBinary, whitelistAscii);

        } catch (IllegalArgumentException iaeeee) {
            LOGGER.info("------------");
            LOGGER.error("ERROR : {}", iaeeee.getLocalizedMessage());
            LOGGER.info("------------");
            exitApplication();
        }

        // check to make sure old directory exists
        File oldDir = new File(sourceDir);
        if(oldDir.exists()) {
            File newDir = new File(targetDir);

            if(forceDelete && newDir.exists()) {
                LOGGER.info("FORCE DELETE ENABLED - Trying to delete: {}", targetDir);
                try {
                    FileUtils.deleteDirectory(newDir);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to delete target directory: " + targetDir + " please delete this directory manually before running the tool again");
                }
            } else if(!forceDelete && newDir.exists()) {
                LOGGER.error("Unable to delete target directory: {} please delete this directory manually before running the tool again", targetDir);
                // hard stop
                exitApplication();
            }

            LOGGER.info("Processing dir: {} ", oldDir.getAbsolutePath());
            LOGGER.info("Size of dir: {} ", FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(oldDir)));

            LOGGER.info("Copying contents to dir: {} ", newDir.getAbsolutePath());
            LOGGER.info("This can take a bit depending on the size of your project .... DO NOT PANIC ... and if you do just delete the target directory and start again !");

            try {
                // kick off the progress bar
                fileVisitor.init();
                // walk the tree using our custom visitor
                Files.walkFileTree(Paths.get(sourceDir), fileVisitor);
                // clean up and close off the progress bar
                fileVisitor.finished();
            } catch (IOException e) {
                LOGGER.error("Unable to copy to target directory: {} something blew up, please validate the contents of your source folder and that the correct target permissions exist", targetName);
                LOGGER.error(e.getLocalizedMessage(), e);
                // hard stop
                exitApplication();
            }

            LOGGER.info("WHEW ! we made it ");

            LOGGER.info("New dir location: {}", newDir.getAbsolutePath());
            LOGGER.info("Size of new dir: {} ", FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(oldDir)));

            exitApplication();
        }
    }

    /**
     * exitApplication - helper method to shut down the app when conditions are not met
     */
    private void exitApplication() {

        LOGGER.info("------------");
        LOGGER.info("  FINISHED  ");
        LOGGER.info("------------");


        int exitCode = SpringApplication.exit(applicationContext, new ExitCodeGenerator() {
            @Override
            public int getExitCode() {
                // no errors
                return 0;
            }
        });
        System.exit(exitCode);
    }
}
