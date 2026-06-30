import sys

with open('src/main/java/com/example/dependencyupdater/settings/DependencyUpdaterSettingsComponent.java', 'r', encoding='utf-8') as f:
    content = f.read()

jfrog_menu_code = '''
        JMenuItem jfrogItem = new JMenuItem("JFrog Artifactory (HTML)");
        jfrogItem.addActionListener(e -> {
            for (Repository r : listModel.getItems()) {
                if (r.getName().trim().equalsIgnoreCase("JFrog Artifactory (HTML)")) {
                    Messages.showErrorDialog(mainPanel, "A repository with the name 'JFrog Artifactory (HTML)' already exists.", "Duplicate Repository");
                    return;
                }
            }
            Repository repo = new Repository("JFrog Artifactory (HTML)", "");
            repo.setHtmlListing(true);
            repo.setAuthType(Repository.AuthType.BEARER);
            listModel.add(repo);
            repositoryList.setSelectedValue(repo, true);
        });
        popup.add(jfrogItem);
        
        popup.addSeparator();

'''

content = content.replace(
    '        popup.addSeparator();',
    jfrog_menu_code
)

with open('src/main/java/com/example/dependencyupdater/settings/DependencyUpdaterSettingsComponent.java', 'w', encoding='utf-8') as f:
    f.write(content)
