/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.repository.gui.actions;

import java.awt.Desktop;
import java.io.File;
import java.security.AccessController;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.repository.BinaryEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.repository.gui.RepositoryTree;
import com.rapidminer.security.PluginSandboxPolicy;
import com.rapidminer.tools.SystemInfoUtilities;


/**
 * This action tries to open the {@link com.rapidminer.repository.BinaryEntry} via the OS open action. For custom
 * behavior, it first checks {@link RepositoryTools#getOpenCommandForSuffix(String)}, and if nothing user-specified is
 * found, will ask the {@link com.rapidminer.repository.gui.OpenBinaryEntryActionRegistry} if a callback is registered.
 * If neither, will try to open via the OS default option.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public class OpenInOperatingSystemAction extends AbstractRepositoryAction<Entry> {

    /** wait that many seconds before aborting the process start */
    private static final int PROCESS_TIMEOUT = 10;

    /** these suffixes prompt an "are you sure?" dialog as running them directly may not be desired */
    private static final List<String> WARNING_SUFFIXES;
    static {
        WARNING_SUFFIXES = Arrays.asList("exe", "bat", "cmd", "com", "vbs", "msi", // Windows
                "sh", "elf", "bin", "deb", // Linux
                "app", "dmg"); // OS X
    }


    public OpenInOperatingSystemAction(RepositoryTree tree) {
        super(tree, Entry.class, false, "repository_open_in_operating_system");
    }

    @Override
    public void actionPerformed(Entry entry) {
        if (entry == null || entry.getLocation() == null) {
            // should not happen
            return;
        }

        if (entry instanceof BinaryEntry) {
            // try to open it
            openInOperatingSystem((BinaryEntry) entry);
        }
    }

    /**
     * Tries to open the given binary entry via the operating system. Only works if it's a local file. This may or may
     * not do anything, depends on OS and various other circumstances. Does so in an async fashion via a {@link
     * ProgressThread}. To try to open a binary entry via the {@link com.rapidminer.repository.gui.OpenBinaryEntryActionRegistry},
     * use {@link com.rapidminer.gui.actions.OpenAction#openBinaryEntryViaRegisteredActionOrInOperatingSystem(BinaryEntry)}.
     *
     * @param entry the entry, must not be {@code null}
     */
    public static void openInOperatingSystem(BinaryEntry entry) {
        String suffix = entry.getSuffix().toLowerCase(Locale.ENGLISH);
        String customCommand = RepositoryTools.getOpenCommandForSuffix(suffix);

        // only signed extensions are allowed to trigger opening files via the OS
        if (System.getSecurityManager() != null) {
            AccessController.checkPermission(new RuntimePermission(PluginSandboxPolicy.RAPIDMINER_INTERNAL_PERMISSION));
        }

        if (customCommand == null && (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN))) {
            SwingTools.showVerySimpleErrorMessage("cannot_open_with_default_program_unsupported_no_custom_command");
            return;
        }

        // it may be dangerous to run it, so ask for confirmation
        if (WARNING_SUFFIXES.contains(suffix) && SwingTools.showConfirmDialog("open_binary.are_you_sure", ConfirmDialog.YES_NO_OPTION, suffix) != ConfirmDialog.YES_OPTION) {
            return;
        }

        new ProgressThread("open_binary") {

            @Override
            public void run() {
                try {
                    File dataFile = entry.toPath().toFile();
                    if (customCommand == null) {
                        try {
                            Desktop.getDesktop().open(dataFile);
                        } catch (Exception e) {
                            // this sometimes fails on Windows for no well explained reason. It works just fine by calling the file in the cmd, though, so try that now
                            if (SystemInfoUtilities.getOperatingSystem() == SystemInfoUtilities.OperatingSystem.WINDOWS) {
                                Process cmd = new ProcessBuilder("cmd.exe", "/C", dataFile.getAbsolutePath()).
                                        redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectError(ProcessBuilder.Redirect.INHERIT).start();
                                if (cmd.waitFor(PROCESS_TIMEOUT, TimeUnit.SECONDS)) {
                                    int exitCode = cmd.exitValue();
                                    if (exitCode != 0) {
                                        SwingTools.showVerySimpleErrorMessage("cannot_open_with_default_program_exitcode", dataFile.getAbsolutePath(), exitCode);
                                    }
                                } else {
                                    // did not stop within 10 seconds, kill it with fire
                                    cmd.destroyForcibly();
                                }
                            } else {
                                // non Windows get an error immediately here now
                                SwingTools.showSimpleErrorMessage("cannot_open_with_default_program", e);
                            }
                        }
                        return;
                    }

                    switch (SystemInfoUtilities.getOperatingSystem()) {
                        case WINDOWS:
                            new ProcessBuilder("cmd.exe", "/C", customCommand, dataFile.getAbsolutePath()).redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectError(ProcessBuilder.Redirect.INHERIT).start();
                            break;
                        case OSX:
                        case UNIX:
                        case SOLARIS:
                            Process p;
                            if (customCommand.startsWith("open ")) {
                                // argument has to be added after open command, so concat
                                p = new ProcessBuilder("sh", "-c", customCommand + " \"" + dataFile.getAbsolutePath() + "\"").
                                        redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectError(ProcessBuilder.Redirect.INHERIT).start();
                            } else {
                                p = new ProcessBuilder("sh", "-c", customCommand, dataFile.getAbsolutePath()).
                                        redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectError(ProcessBuilder.Redirect.INHERIT).start();
                            }
                            int exitCode = p.waitFor(PROCESS_TIMEOUT, TimeUnit.SECONDS) ? p.exitValue() : 0;
                            switch (exitCode) {
                                case 0:
                                    // all good
                                    // some programs don't return control and thus we don't know if it started or not.
                                    break;
                                case 127:
                                    // unknown command/location
                                    SwingTools.showVerySimpleErrorMessage("cannot_open_with_default_program_127", suffix, exitCode);
                                    break;
                                default:
                                    SwingTools.showVerySimpleErrorMessage("cannot_open_with_default_program_exitcode", suffix, exitCode);
                            }
                            break;
                        case OTHER:
                        default:
                            SwingTools.showVerySimpleErrorMessage("cannot_open_with_default_program_unknown_os");
                    }
                } catch (Exception e) {
                    SwingTools.showSimpleErrorMessage("cannot_open_with_default_program", e);
                }
            }
        }.start();
    }
}