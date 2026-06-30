import sys

with open('src/main/resources/META-INF/plugin.xml', 'r', encoding='utf-8') as f:
    content = f.read()

old_notes = '''        <li><b>Corporate Proxy & SSL:</b> Migrated underlying HTTP engine to IntelliJ native <code>HttpRequests</code> for robust handling of Enterprise Proxies and Custom SSL Certificates (JFrog).</li>
        <li><b>Improved History View:</b> Reintroduced visibility of older versions in the dropdown list to accurately reflect past security vulnerabilities (CVEs).</li>
        <li><b>Bugfixes:</b> Patched minor UI glitches and optimized regex matchers for <code>libs.versions.toml</code> syntax.</li>'''

new_notes = '''        <li><b>Corporate Proxy & SSL:</b> Migrated underlying HTTP engine to IntelliJ native <code>HttpRequests</code> for robust handling of Enterprise Proxies and Custom SSL Certificates.</li>
        <li><b>JFrog Artifactory (HTML):</b> Dedicated predefined repository type for private JFrog instances utilizing HTML directory listings. Automatically scans both <code>libs-release</code> and <code>libs-snapshot</code> concurrently.</li>
        <li><b>Improved History View:</b> Reintroduced visibility of older versions in the dropdown list to accurately reflect past security vulnerabilities (CVEs).</li>
        <li><b>Bugfixes:</b> Patched minor UI glitches, introduced dynamic authentication labels, and optimized regex matchers for <code>libs.versions.toml</code> syntax.</li>'''

content = content.replace(old_notes, new_notes)

with open('src/main/resources/META-INF/plugin.xml', 'w', encoding='utf-8') as f:
    f.write(content)
