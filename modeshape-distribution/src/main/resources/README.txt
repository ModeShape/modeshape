# Overview

ModeShape 3.0 is the first JCR content repository that is seriously fast, extremely scalable, and highly available.
It's easy and lightweight enough to embed into small application, but also powerful enough to build a large peer-to-peer
cluster capable of storing huge volumes of content and supporting many concurrent and active clients.
It's fully open source with a business-friendly license and written in Java, so it can run almost anywhere.

For the complete documentation, please visit: https://docs.jboss.org/author/display/MODE/Home

# Using Maven

By far the easiest way to use ModeShape is to use Maven, because with just a few lines of code, Maven will automatically pull
all the JARs and source for all of the ModeShape libraries as well as everything those libraries need.

For all the details, please visit http://www.jboss.org/modeshape/downloads/maven

# Distribution Structure

The modeshape-dist.zip contains the following structure:

/- modeshape-version

    /- doc - the documentations folder, which will contain among other things, the full javadoc

    /- examples - the example folder, which contains out-of-the-box, runnable examples. See below for more information

        /- example_1
        /- example_2

    /- lib - the folder containing all the runtime-dependencies of the core components

    /- sources - the folder which contains jars with the core component's sources and test sources

    /- tests - the folder which contains the core component's compiled tests

    /- modules - the root folder of all the "extra" modules, which can be used on top of ModeShape's core.

        /- module_1

            /- lib - the folder containing the module's specific dependencies
            /- sources - the folder with the module's sources and test sources
            /- tests - the folder with the module's compiled tests
            - any additional readme(s), copyright and authors file, specific to any module

       /- module_2

   - modeshape-core-jars - represent the jars which constitute ModeShape's core.
                           These are the minimum required ModeShape dependencies when running standalone or inside a container
                           not yet supported by ModeShape.

   - AUTHORS.txt
   - COPYRIGHT.txt
   - README.txt

# Running the Examples

ModeShape's distribution comes with a number of pre-packaged, ready-to-run examples. Each of them is packaged inside its own
folder and contains, inside this folder the Windows and Linux scripts necessary to run the examples (by default, the scripts are called run.cmd/run.sh).

To run any example, just go to its root folder and execute one of the above scripts.