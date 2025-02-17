package ch.gatzka.view;

import ch.gatzka.repository.view.LatestReportViewRepository;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.ZoneOffset;

@PageTitle("Feed")
@Route("")
@Menu(order = 0, icon = LineAwesomeIconUrl.HISTORY_SOLID)
@AnonymousAllowed
public class FeedView extends VerticalLayout {

    private final LatestReportViewRepository latestReportViewRepository;

    public FeedView(LatestReportViewRepository latestReportViewRepository) {
        this.latestReportViewRepository = latestReportViewRepository;

        MessageList messageList = new MessageList();

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setAlignSelf(FlexComponent.Alignment.START, messageList);


        setMessageListSampleData(messageList);

        add(messageList);
    }

    private void setMessageListSampleData(MessageList messageList) {
        messageList.setItems(latestReportViewRepository.readAll().map(report ->
                new MessageListItem(
                        "Reported %s items found inside %s with a total value of ₽ %s".formatted(report.getItemCount(), report.getName(), report.getTotalValue()),
                        report.getReportedAt().toInstant(ZoneOffset.UTC), "User#%s".formatted(report.getReportedBy()),
                        report.getImageLink()
                )
        ));
    }


}
