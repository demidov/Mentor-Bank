package ru.mentorbank.backoffice.services.moneytransfer;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.mentorbank.backoffice.dao.OperationDao;
import ru.mentorbank.backoffice.dao.exception.OperationDaoException;
import ru.mentorbank.backoffice.dao.stub.OperationDaoStub;
import ru.mentorbank.backoffice.model.Operation;
import ru.mentorbank.backoffice.model.stoplist.*;
import ru.mentorbank.backoffice.model.transfer.*;
import ru.mentorbank.backoffice.services.accounts.AccountService;
import ru.mentorbank.backoffice.services.accounts.AccountServiceBean;
import ru.mentorbank.backoffice.services.moneytransfer.exceptions.TransferException;
import ru.mentorbank.backoffice.services.stoplist.StopListService;
import ru.mentorbank.backoffice.services.stoplist.StopListServiceStub;
import ru.mentorbank.backoffice.test.AbstractSpringTest;

public class MoneyTransferServiceTest extends AbstractSpringTest {

	@Autowired
	private MoneyTransferServiceBean moneyTransferService;
	private JuridicalAccountInfo srcAccountInfo;
	private TransferRequest transferRequest;
	private PhysicalAccountInfo dstAccountInfo;
	private StopListInfo stopListInfo;

	@Before
	public void setUp() {
		srcAccountInfo = new JuridicalAccountInfo();
		srcAccountInfo.setAccountNumber("1");
		srcAccountInfo.setInn(StopListServiceStub.INN_FOR_OK_STATUS);

		dstAccountInfo = new PhysicalAccountInfo();
		dstAccountInfo.setAccountNumber("2");
		dstAccountInfo.setDocumentNumber(StopListServiceStub.DOCUMENT_NUMBER_FOR_OK_STATUS);
		dstAccountInfo.setDocumentSeries("2-2");
		dstAccountInfo.setFirstname("Ivanov");
		dstAccountInfo.setLastname("Ivan");
		dstAccountInfo.setMiddlename("Ivanovich");

		transferRequest = new TransferRequest();
		transferRequest.setSrcAccount(srcAccountInfo);
		transferRequest.setDstAccount(dstAccountInfo);

		stopListInfo = new StopListInfo();
		stopListInfo.setStatus(StopListStatus.OK);
		stopListInfo.setComment("Comment");
	}

	@Test
	public void transfer() throws TransferException, OperationDaoException {
		StopListService mockedStopListService = mock(StopListServiceStub.class);
		when(mockedStopListService.getJuridicalStopListInfo(any(JuridicalStopListRequest.class))).thenReturn(stopListInfo);
		when(mockedStopListService.getPhysicalStopListInfo(any(PhysicalStopListRequest.class))).thenReturn(stopListInfo);

		AccountService mockedAccountService = mock(AccountServiceBean.class);
		when(mockedAccountService.verifyBalance(srcAccountInfo)).thenReturn(true);

		OperationDao mockedOperationDao = mock(OperationDaoStub.class);

		moneyTransferService.setAccountService(mockedAccountService);
		moneyTransferService.setStopListService(mockedStopListService);
		moneyTransferService.setOperationDao(mockedOperationDao);

		moneyTransferService.transfer(transferRequest);

		verify(mockedAccountService).verifyBalance(srcAccountInfo);
		verify(mockedStopListService).getJuridicalStopListInfo(any(JuridicalStopListRequest.class));
		verify(mockedStopListService).getPhysicalStopListInfo(any(PhysicalStopListRequest.class));
		verify(mockedOperationDao).saveOperation(any(Operation.class));
	}
}
