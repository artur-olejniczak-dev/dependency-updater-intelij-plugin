package com.example.dependencyupdater;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PomParserMockitoTest {

    @Mock
    private Project mockProject;

    @Mock
    private XmlFile mockXmlFile;

    @Mock
    private XmlTag mockRootTag;

    @Mock
    private XmlTag mockDependenciesTag;

    @Mock
    private XmlTag mockDependencyTag;

    @Mock
    private XmlTag mockGroupIdTag;

    @Mock
    private XmlTag mockArtifactIdTag;

    @Mock
    private XmlTag mockVersionTag;

    @Mock
    private com.intellij.psi.xml.XmlTagValue mockGroupIdValue;

    @Mock
    private com.intellij.psi.xml.XmlTagValue mockArtifactIdValue;

    @Mock
    private com.intellij.psi.xml.XmlTagValue mockVersionValue;

    @Test
    void testExtractDependenciesWithMocks() {
        when(mockXmlFile.getRootTag()).thenReturn(mockRootTag);
        when(mockRootTag.findFirstSubTag("dependencies")).thenReturn(mockDependenciesTag);
        when(mockDependenciesTag.findSubTags("dependency")).thenReturn(new XmlTag[]{mockDependencyTag});

        when(mockDependencyTag.findFirstSubTag("groupId")).thenReturn(mockGroupIdTag);
        when(mockGroupIdTag.getValue()).thenReturn(mockGroupIdValue);
        when(mockGroupIdValue.getTrimmedText()).thenReturn("org.springframework");

        when(mockDependencyTag.findFirstSubTag("artifactId")).thenReturn(mockArtifactIdTag);
        when(mockArtifactIdTag.getValue()).thenReturn(mockArtifactIdValue);
        when(mockArtifactIdValue.getTrimmedText()).thenReturn("spring-core");

        when(mockDependencyTag.findFirstSubTag("version")).thenReturn(mockVersionTag);
        when(mockVersionTag.getValue()).thenReturn(mockVersionValue);
        when(mockVersionValue.getTrimmedText()).thenReturn("5.3.9");

        try (MockedStatic<FilenameIndex> filenameIndexMock = Mockito.mockStatic(FilenameIndex.class);
             MockedStatic<GlobalSearchScope> scopeMock = Mockito.mockStatic(GlobalSearchScope.class);
             MockedStatic<com.intellij.psi.PsiManager> psiManagerMock = Mockito.mockStatic(com.intellij.psi.PsiManager.class)) {
             
            GlobalSearchScope mockScope = mock(GlobalSearchScope.class);
            scopeMock.when(() -> GlobalSearchScope.projectScope(mockProject)).thenReturn(mockScope);
            
            com.intellij.openapi.vfs.VirtualFile mockVirtualFile = mock(com.intellij.openapi.vfs.VirtualFile.class);
            java.util.Collection<com.intellij.openapi.vfs.VirtualFile> vFiles = java.util.Collections.singletonList(mockVirtualFile);
            
            filenameIndexMock.when(() -> FilenameIndex.getVirtualFilesByName("pom.xml", mockScope))
                             .thenReturn(vFiles);
                             
            com.intellij.psi.PsiManager mockPsiManager = mock(com.intellij.psi.PsiManager.class);
            psiManagerMock.when(() -> com.intellij.psi.PsiManager.getInstance(mockProject)).thenReturn(mockPsiManager);
            when(mockPsiManager.findFile(mockVirtualFile)).thenReturn(mockXmlFile);

            PomParser parser = new PomParser();
            List<Dependency> extracted = parser.extractDependencies(mockProject);

            assertEquals(1, extracted.size());
            assertEquals("org.springframework", extracted.get(0).getGroupId());
            assertEquals("spring-core", extracted.get(0).getArtifactId());
            assertEquals("5.3.9", extracted.get(0).getCurrentVersion());
        }
    }
}
