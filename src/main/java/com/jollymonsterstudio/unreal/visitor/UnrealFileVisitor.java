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

package com.jollymonsterstudio.unreal.visitor;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

public class UnrealFileVisitor extends SimpleFileVisitor<Path> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnrealFileVisitor.class);

    private static final List<String> DEFAULT_DIR_WHITELIST = Arrays.asList("Config", "Content", "Source");
    private static final List<String> DEFAULT_BINARY_FILE_WHITELIST =  Arrays.asList("uasset", "png", "jpg", "jpeg", "wav", "umap");
    private static final List<String> DEFAULT_ASCII_FILE_WHITELIST = Arrays.asList("ini", "cpp", "h", "uproject", "sln", "cs", "gitignore", "md", "txt");

    private enum FileType {
        BINARY, ASCII
    }

    private Path source;
    private Path target;

    private String oldProjectName;
    private String newProjectName;

    private ProgressBar progressBar;

    private boolean processingChildDir = false;
    private String childDir = null;

    private List<String> whitelistDirs;
    private List<String> whitelistBinary;
    private List<String> whitelistAscii;

    private List<String> ignoredDirs = new ArrayList<>();
    private final Pattern pattern = Pattern.compile("(?<=\\bNewClassName=\")[^\"]*");

    /**
     * UnrealFileVisitor
     * @param source - the source directory we will copy from
     * @param oldProjectName - the name of the original project
     * @param target - the new location for our copied project
     * @param newProjectName - the new name for our project
     * @param whitelistDirs - any directories we want to include in the copy
     * @param whitelistBinary - any binary files we want to include in the copy ( binary files are not examined / modified just copied verbatim )
     * @param whitelistAscii - any ascii files we want to include in the copy and push through the renaming process
     */
    public UnrealFileVisitor(final Path source, final String oldProjectName, final Path target, final String newProjectName, final String[] whitelistDirs, final String[] whitelistBinary, final String[] whitelistAscii) {
        this.source = source;
        this.target = target;

        this.oldProjectName = oldProjectName;
        this.newProjectName = newProjectName;

        // default if no parameters are included so the basic components are copied
        this.whitelistDirs = ArrayUtils.isNotEmpty(whitelistDirs) ? Arrays.asList(whitelistDirs) : DEFAULT_DIR_WHITELIST;
        this.whitelistBinary = ArrayUtils.isNotEmpty(whitelistBinary) ? Arrays.asList(whitelistBinary) : DEFAULT_BINARY_FILE_WHITELIST;
        this.whitelistAscii = ArrayUtils.isNotEmpty(whitelistAscii) ? Arrays.asList(whitelistAscii) : DEFAULT_ASCII_FILE_WHITELIST;
    }

    /**
     * init - really only here so we can initialize the progress bar outside of the constructor, aesthetics reasons
     */
    public void init() {
        // grab a count of all the files for our progress bar
        Collection<File> files = FileUtils.listFilesAndDirs(source.toFile(), TrueFileFilter.TRUE, TrueFileFilter.TRUE);
        if(!CollectionUtils.isEmpty(files)) {
            // the -1 is to ignore the root folder so your counts match when examining file explorer
            progressBar = new ProgressBar("File Copy Progress: ", files.size() - 1, ProgressBarStyle.ASCII);
        }
    }

    /**
     * finished - similar reason to the init, need to kill the progress bar
     */
    public void finished() {
        progressBar.close();
    }

    /**
     * preVisitDirectory - allows us to examine each directory before we process it.
     *                     in our case we use it to retrieve information about the source
     *                     and move those details over to the target directories
     * @param dir the current directory we are processing
     * @param attrs any file attributes used for additional filtering
     * @return {@see FileVisitResult}
     * @throws IOException in case touching the disk blows up
     */
    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
        // throw this into a mutable variable
        Path currentPath = dir;

        // check against whitelist
        if(currentPath != null && currentPath.toFile().isDirectory()) {
            // used to toll gate which files we process
            boolean proceed = false;
            // check to see if we are a child of a root folder if so just move on
            if(processingChildDir && StringUtils.isNotEmpty(childDir) && currentPath.toAbsolutePath().toString().contains(childDir)) {
                proceed = true;
            } else {
                // root dir when we first start off
                if(dir.equals(source)) {
                    processingChildDir = false;
                    childDir = null;
                    proceed = true;
                    // start checking against our white list for valid directories
                } else if((whitelistDirs.contains(currentPath.getFileName().toString()) && currentPath.getParent().toAbsolutePath().equals(source.toAbsolutePath()))) {
                    // allowed dir based on white list
                    processingChildDir = true;
                    childDir = currentPath.getFileName().toString();
                    proceed = true;
                } else {
                    processingChildDir = false;
                    childDir = null;
                }
            }
            // if we are good to go we can now examine the folders
            if(proceed) {
                // create the new directory based on inital params
                Path newDirectory = target.resolve(source.relativize(currentPath));
                // check to see if directory contains references to old project name
                if(processingChildDir && currentPath.getFileName().toString().contains(oldProjectName)) {
                    // rename accordingly if found
                    String newPath = newDirectory.toAbsolutePath().toString().substring(0, newDirectory.toAbsolutePath().toString().lastIndexOf(File.separator));
                    newDirectory = Paths.get(newPath).resolve(currentPath.getFileName().toString().replace(oldProjectName, newProjectName));
                }

                // lastly trigger the copy against the new folder name
                try{
                    Files.copy(currentPath, newDirectory);
                }
                catch (FileAlreadyExistsException ioException){
                    LOGGER.error("Dir or file already exist: {}", ioException.getLocalizedMessage());
                    //log it and move on
                    return SKIP_SUBTREE; // skip processing
                }
            } else {
                // if we find nothing satisfying our whitelist add it to
                // the ignore folder structure for our files to use
                ignoredDirs.add(currentPath.toAbsolutePath().toString());
            }
        }
        // tick the progress bar
        progressBar.step();
        return CONTINUE;
    }

    /**
     * visitFile - allows us to examine file before we process it.
     *                     in our case we use it to retrieve information about the source
     *                     and move those details over to the target file
     * @param file the current file we are processing
     * @param attrs any file attributes used for additional filtering
     * @return {@see FileVisitResult}
     */
    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
        // mutable variable
        Path currentPath = file;

        // skip any file that belongs to an ignored dir
        String absoluteFilePath = currentPath.toAbsolutePath().toString();
        for(String ignoredDir : ignoredDirs) {
            // special case for gitignore
            if(absoluteFilePath.startsWith(ignoredDir) && !absoluteFilePath.contains(".gitignore")) {
                currentPath = null;
                break;
            }
        }

        if(currentPath != null) {
            // lets start checking our extensions against the white list
            String extension = FilenameUtils.getExtension(absoluteFilePath);
            if(StringUtils.isNotEmpty(extension)) {
                // check if file is binary or ascii in which case we can update the contents of the ascii ones
                FileType fileType = null;
                if(whitelistBinary.contains(extension)) {
                    fileType = FileType.BINARY;
                } else if(whitelistAscii.contains(extension)) {
                    fileType = FileType.ASCII;
                }

                if(fileType != null) {
                    // create the new file path
                    Path newFile = target.resolve(source.relativize(currentPath));
                    // examine new path to see if we have any old project names
                    if(newFile.toAbsolutePath().toString().contains(oldProjectName)) {
                        // remove any extra trailing slashes and clean up the folder name
                        String newFilePath = newFile.toString().substring(target.toString().length() + 1, newFile.toString().length());
                        newFilePath = newFilePath.replace(oldProjectName, newProjectName);

                        newFile = Paths.get(target.toString()).resolve(newFilePath);
                    }

                    try{
                        // finally copy over the file to the new location
                        Files.copy(file, newFile);

                        // if we have ascii files we still have work to do
                        if(FileType.ASCII.equals(fileType)) {
                            // bring in the new file
                            String content = FileUtils.readFileToString(newFile.toFile(), Charset.defaultCharset());
                            // start replacing old project names
                            String updatedContent = content.replace(oldProjectName, newProjectName);
                            updatedContent = updatedContent.replace(oldProjectName.toUpperCase()+"_API", newProjectName.toUpperCase()+"_API");

                            // do some special work if we encounter the DefaultEngine.ini file
                            // as in order to get it running we need to account for ActiveClassRedirects as the blueprints
                            // and other binary assets still retain references to those old project definitions
                            // some more reading about it here https://forums.unrealengine.com/development-discussion/blueprint-visual-scripting/24493-migrate-code-based-blueprint
                            if(newFile.getFileName().toString().contains("DefaultEngine.ini")) {
                                // let's bring in the old file
                                String oldContent = FileUtils.readFileToString(file.toFile(), Charset.defaultCharset());
                                String[] oldIniLines = oldContent.split(System.getProperty("line.separator"));

                                List<String> oldRedirects = new ArrayList<>();
                                // figure out where the redirects occur
                                for(String line : oldIniLines) {
                                    if(line.contains("ActiveClassRedirects")) {
                                        // if we find them lets add them to our list
                                        Matcher matcher = pattern.matcher(line);
                                        while (matcher.find()) {
                                            oldRedirects.add(matcher.group());
                                            break;
                                        }
                                    }
                                }

                                // start building out the new DefaultEngine.ini contents
                                String[] newIniLines = updatedContent.split(System.getProperty("line.separator"));
                                StringBuilder stringBuilder = new StringBuilder();
                                // process each line of the file
                                for(String line : newIniLines) {
                                    stringBuilder.append(line);
                                    stringBuilder.append(System.getProperty("line.separator"));
                                    // add in our redirects
                                    if(line.equalsIgnoreCase("[/Script/Engine.Engine]")) {
                                        stringBuilder.append(System.getProperty("line.separator"));
                                        // update references to classes / blueprints
                                        // create new redirect for project name
                                        stringBuilder.append("+ActiveGameNameRedirects=(OldGameName=\"/Script/").append(oldProjectName).append("\", NewGameName=\"/Script/").append(newProjectName).append("\")");
                                        stringBuilder.append(System.getProperty("line.separator"));
                                        // if we have any class specific redirects add them in
                                        if(!oldRedirects.isEmpty()) {
                                            for(String redirect : oldRedirects) {
                                                stringBuilder.append("+ActiveGameNameRedirects=(OldGameName=\"/Script/").append(oldProjectName).append(".").append(redirect).append("\", NewGameName=\"/Script/").append(newProjectName).append(".").append(redirect.replace(oldProjectName, newProjectName)).append("\")");
                                                stringBuilder.append(System.getProperty("line.separator"));
                                            }
                                        }
                                    }
                                }
                                // build our final string
                                updatedContent = stringBuilder.toString();
                            }

                            // persist contents to our new file
                            FileUtils.writeStringToFile(newFile.toFile(), updatedContent, Charset.defaultCharset());
                        }
                    }
                    catch (IOException ioException){
                        //log it and move
                        LOGGER.error(ioException.getLocalizedMessage(), ioException);
                    }
                }

            }
        }
        // tick the progress bar
        progressBar.step();
        return CONTINUE;

    }

    /**
     * postVisitDirectory - allows us to examine a directory after it's been processed. NOT USED
     * @param dir the current dir we processed
     * @param exc {@see IOException}
     * @return {@see FileVisitResult}
     */
    @Override
    public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) {
        if(exc != null) {
            LOGGER.error(exc.getLocalizedMessage(), exc);
        }
        return CONTINUE;
    }

    /**
     * visitFileFailed - can handle errors. NOT USED
     * @param file the current file we processed
     * @param exc {@see IOException}
     * @return {@see FileVisitResult}
     */
    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException exc) {
        if(exc != null) {
            LOGGER.error(exc.getLocalizedMessage(), exc);
        }
        return CONTINUE;
    }
}
