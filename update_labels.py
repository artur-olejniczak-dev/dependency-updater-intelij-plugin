import sys

with open('src/main/java/com/example/dependencyupdater/settings/DependencyUpdaterSettingsComponent.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_logic = '''        usernameField.setVisible(isBasic);
        usernameLabel.setVisible(isBasic);
        
        passwordField.setVisible(isBasic || isBearer);
        passwordLabel.setVisible(isBasic || isBearer);'''

new_logic = '''        usernameField.setVisible(isBasic);
        usernameLabel.setVisible(isBasic);
        
        passwordField.setVisible(isBasic || isBearer);
        passwordLabel.setVisible(isBasic || isBearer);
        
        if (isBasic) {
            passwordLabel.setText("Password:");
        } else if (isBearer) {
            passwordLabel.setText("Token:");
        } else {
            passwordLabel.setText("Password / Token:");
        }'''

content = content.replace(old_logic, new_logic)

with open('src/main/java/com/example/dependencyupdater/settings/DependencyUpdaterSettingsComponent.java', 'w', encoding='utf-8') as f:
    f.write(content)
