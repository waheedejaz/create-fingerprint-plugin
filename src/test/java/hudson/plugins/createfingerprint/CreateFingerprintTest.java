/*
 *  The MIT License
 *
 *  Copyright 2011-2012 Marc Sanfacon
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package hudson.plugins.createfingerprint;

import hudson.model.Fingerprint;
import hudson.model.FreeStyleProject;
import hudson.model.FreeStyleBuild;
import hudson.tasks.Fingerprinter;
import hudson.tasks.Fingerprinter.FingerprintAction;
import org.junit.Test;
import org.junit.Before;
import static hudson.cli.CLICommandInvoker.Matcher.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import java.util.Collection;
import java.io.*;

/**
 * @author Marc Sanfacon
 */
public class CreateFingerprintTest {
    
    @Rule
    public JenkinsRule j = new JenkinsRule();

    private static final String JOB1 = "job1";

    private FreeStyleProject createJob(String jobName) {
        try {
            final FreeStyleProject job = j.createFreeStyleProject(jobName);
            return job;
        } catch (Exception ex) {
            assertThat("Unexpected exception: " + ex, false);
        }
        return null;
    }

    private FreeStyleBuild assertStatusAndGetBuild(FreeStyleProject job) {
        try {
            FreeStyleBuild build = j.assertBuildStatusSuccess(job.scheduleBuild2(0));
            return build;
        } catch (Exception e) {
            assertThat("Unexpected exception: " + e, false);
        }
        return null;
    }

    @Test
    public void testConfigRoundtrip() {

        // Create a test file to create the fingerprint
        try {
            FileWriter fstream = new FileWriter("test.txt");
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("Fingerprint");
            out.close();
        } catch (Exception e) {
            assertThat("Unexpected exception: " + e, false);
        }

        final FreeStyleProject project = createJob(JOB1);
        CreateFingerprint before = new CreateFingerprint("test.txt");
        project.getBuildersList().add(before);

        try {
            j.configRoundtrip(project);
        } catch (Exception e) {
            assertThat("Unexpected exception: " + e, false);
        }

        CreateFingerprint after = project.getBuildersList().get(CreateFingerprint.class);

        // Verify that the build runs correctly and that the Fingerprints are created
        assertThat(before, not(after));

        try {
            j.assertEqualDataBoundBeans(before,after);
        } catch (Exception e) {
            assertThat("Unexpected exception: " + e, false);
        }
        
        assertThat(after.getTargets(), is(equalTo("test.txt")));
        
        FreeStyleBuild build = assertStatusAndGetBuild(project);
        String buildName = project.getName();
        Fingerprinter.FingerprintAction action = build.getAction(Fingerprinter.FingerprintAction.class);
        Collection<Fingerprint> fingerprints = action.getFingerprints().values();
        for (Fingerprint f: fingerprints) {
            assertThat(f.getOriginal().is(build), is(true));
            assertThat(f.getOriginal().getName().equals(buildName), is(true));
        }
    }
}
