WildFly BOM Builder Plugin Contributing Guide
=============================================

WildFly BOM Builder Plugin is a maven plugin that builds Wildfly BOMs.

Basic Steps
-----------

To contribute to WildFly BOM Builder Plugin, fork the wildfly-bom-builder-plugin repository to your own Git, clone your fork, commit your work on topic branches, and make pull requests.

If you don't have the Git client (`git`), get it from: <http://git-scm.com/>

Here are the steps in detail:

1. [Fork](https://github.com/wildfly/wildfly-bom-builder-plugin/fork_select) the project. This creates a the project in your own Git.

2. Clone your fork. This creates a directory in your local file system.

        git clone git@github.com:<your-username>/wildfly-bom-builder-plugin.git

3. Add the remote `upstream` repository.

        git remote add upstream git@github.com:wildfly/wildfly-bom-builder-plugin.git

4. Get the latest files from the `upstream` repository.

        git fetch upstream

5. Create a new topic branch to contain your features, changes, or fixes.

        git checkout -b <topic-branch-name> upstream/main

6. Contribute new code or make changes to existing files. Make sure that you follow the General Guidelines below.

7. Build the project and install the boms into your Maven repository.

        mvn clean install


7. Test the changes using Maven.

    1. Navigate to the root of the project and run `mvn test`.
    2. Verify the generated project builds and runs as expected.

8. Commit your changes to your local topic branch. You must use `git add filename` for every file you create or change.

        git add <changed-filename>
        git commit -m `Description of change...`

9. Push your local topic branch to your github forked repository. This will create a branch on your Git fork repository with the same name as your local topic branch name.

        git push origin HEAD

10. Browse to the <topic-branch-name> branch on your forked Git repository and [open a Pull Request](http://help.github.com/send-pull-requests/). Give it a clear title and description.

License Information and Contributor Agreement
---------------------------------------------

  WildFly BOM Builder Plugin is licensed under the Apache License 2.0.

  There is no need to sign a contributor agreement to contribute to WildFly BOM Builder Plugin. You just need to explicitly license any contribution under the AL 2.0. If you add any new files to WildFly BOM Builder Plugin, make sure to add the correct header.

### Java

      /*
       * JBoss, Home of Professional Open Source
       * Copyright <Year>, Red Hat, Inc. and/or its affiliates, and individual
       * contributors by the @authors tag. See the copyright.txt in the
       * distribution for a full listing of individual contributors.
       *
       * Licensed under the Apache License, Version 2.0 (the "License");
       * you may not use this file except in compliance with the License.
       * You may obtain a copy of the License at
       * http://www.apache.org/licenses/LICENSE-2.0
       * Unless required by applicable law or agreed to in writing, software
       * distributed under the License is distributed on an "AS IS" BASIS,
       * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
       * See the License for the specific language governing permissions and
       * limitations under the License.
       */

### XML

      <!--
       JBoss, Home of Professional Open Source
       Copyright <Year>, Red Hat, Inc. and/or its affiliates, and individual
       contributors by the @authors tag. See the copyright.txt in the
       distribution for a full listing of individual contributors.

       Licensed under the Apache License, Version 2.0 (the "License");
       you may not use this file except in compliance with the License.
       You may obtain a copy of the License at
       http://www.apache.org/licenses/LICENSE-2.0
       Unless required by applicable law or agreed to in writing, software
       distributed under the License is distributed on an "AS IS" BASIS,
       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
       See the License for the specific language governing permissions and
       limitations under the License.
       -->

### Properties files

       # JBoss, Home of Professional Open Source
       # Copyright <Year>, Red Hat, Inc. and/or its affiliates, and individual
       # contributors by the @authors tag. See the copyright.txt in the
       # distribution for a full listing of individual contributors.
       #
       # Licensed under the Apache License, Version 2.0 (the "License");
       # you may not use this file except in compliance with the License.
       # You may obtain a copy of the License at
       # http://www.apache.org/licenses/LICENSE-2.0
       # Unless required by applicable law or agreed to in writing, software
       # distributed under the License is distributed on an "AS IS" BASIS,
       # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
       # See the License for the specific language governing permissions and
       # limitations under the License.


