package com.example.dependencyupdater;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class DependencyUpdaterToolWindowFactoryTest {
    @Mock
    private Project mockProject;
    @Mock
    private ToolWindow mockToolWindow;
    @Mock
    private ContentManager mockContentManager;
    @Mock
    private ContentFactory mockContentFactory;
    @Mock
    private Content mockContentDirect;
    @Mock
    private Content mockContentTransitive;
    @Test
    void testCreateToolWindowContent() {
        when(mockToolWindow.getContentManager()).thenReturn(mockContentManager);
        try (MockedStatic<ContentFactory> contentFactoryStatic = Mockito.mockStatic(ContentFactory.class)) {
            contentFactoryStatic.when(ContentFactory::getInstance).thenReturn(mockContentFactory);
            when(mockContentFactory.createContent(any(DependencyUpdaterPanel.class), eq("Direct Dependencies"), eq(false)))
                    .thenReturn(mockContentDirect);
            when(mockContentFactory.createContent(any(TransitiveDependencyPanel.class), eq("Transitive Dependencies"), eq(false)))
                    .thenReturn(mockContentTransitive);
            DependencyUpdaterToolWindowFactory factory = new DependencyUpdaterToolWindowFactory();
            factory.createToolWindowContent(mockProject, mockToolWindow);
            verify(mockContentManager, times(1)).addContent(mockContentDirect);
            verify(mockContentManager, times(1)).addContent(mockContentTransitive);
        }
    }
}
