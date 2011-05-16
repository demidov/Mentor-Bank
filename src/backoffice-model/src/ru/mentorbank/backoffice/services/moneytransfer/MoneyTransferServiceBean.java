package ru.mentorbank.backoffice.services.moneytransfer;

import java.util.Calendar;

import ru.mentorbank.backoffice.dao.OperationDao;
import ru.mentorbank.backoffice.dao.exception.OperationDaoException;
import ru.mentorbank.backoffice.model.Account;
import ru.mentorbank.backoffice.model.Operation;
import ru.mentorbank.backoffice.model.stoplist.*;
import ru.mentorbank.backoffice.model.transfer.*;
import ru.mentorbank.backoffice.services.accounts.AccountService;
import ru.mentorbank.backoffice.services.moneytransfer.exceptions.TransferException;
import ru.mentorbank.backoffice.services.stoplist.StopListService;

public class MoneyTransferServiceBean implements MoneyTransferSerice {

	public static final String LOW_BALANCE_ERROR_MESSAGE = "Can not transfer money, because of low balance in the source account";
	private AccountService accountService;
	private StopListService stopListService;
	private OperationDao operationDao;

	public void transfer(TransferRequest request) throws TransferException {
		// Создаём новый экземпляр внутреннего класса, для того, чтобы можно
		// было хранить в состоянии объекта информацию по каждому запросу.
		// Так как MoneyTransferServiceBean конфигурируется как singleton
		// scoped, то в нём нельзя хранить состояние уровня запроса из-за
		// проблем параллельного доступа.
		new MoneyTransfer(request).transfer();
	}

	class MoneyTransfer {

		private TransferRequest request;
		private StopListInfo srcStopListInfo;
		private StopListInfo dstStopListInfo;

		public MoneyTransfer(TransferRequest request) {
			this.request = request;
		}

		public void transfer() throws TransferException {
			verifySrcBalance();
			initializeStopListInfo();
			saveOperation();
			if (isStopListInfoOK()) {
				transferDo();
				removeSuccessfulOperation();
			} else
				throw new TransferException(
						"Невозможно сделать перевод. Необходимо ручное вмешательство.");
		}

		/**
		 * Если операция перевода прошла, то её нужно удалить из таблицы
		 * операций для ручного вмешательства
		 */
		private void removeSuccessfulOperation() {

		}

		private void initializeStopListInfo() {
			srcStopListInfo = getStopListInfo(request.getSrcAccount());
			dstStopListInfo = getStopListInfo(request.getDstAccount());
		}

		private void saveOperation() throws TransferException {
			Operation operation = createNewOperation();
			try {
				operationDao.saveOperation(operation);
			} catch (OperationDaoException e) {
				throw new TransferException(e.getMessage());
			}
		}
		
		private Operation createNewOperation(){
			Operation operation = new Operation();
			operation.setCreateDate(Calendar.getInstance());
			operation.setSrcStoplistInfo(srcStopListInfo);
			operation.setDstStoplistInfo(dstStopListInfo);

			Account srcAccount = new Account();
			srcAccount.setAccountNumber(request.getSrcAccount().getAccountNumber());
			Account dstAccount = new Account();
			dstAccount.setAccountNumber(request.getDstAccount().getAccountNumber());

			operation.setSrcAccount(srcAccount);
			operation.setDstAccount(dstAccount);
			return operation;
		}

		private void transferDo() throws TransferException {
			// Эту операцию пока не реализовавем. Она должна вызывать
			// CDCMoneyTransferServiceConsumer которого ещё нет
		}

		private boolean isStopListInfoOK() {
			if (StopListStatus.OK.equals(srcStopListInfo.getStatus())
					&& StopListStatus.OK.equals(dstStopListInfo.getStatus())) {
				return true;
			}
			return false;
		}

		private StopListInfo getStopListInfo(AccountInfo accountInfo) {
			if (accountInfo instanceof JuridicalAccountInfo) {
				JuridicalAccountInfo juridicalAccountInfo = (JuridicalAccountInfo) accountInfo;
				JuridicalStopListRequest request = new JuridicalStopListRequest();
				request.setInn(juridicalAccountInfo.getInn());
				StopListInfo stopListInfo = stopListService
						.getJuridicalStopListInfo(request);
				return stopListInfo;
			} else if (accountInfo instanceof PhysicalAccountInfo) {
				return stopListService
						.getPhysicalStopListInfo(getPhysicalStopListRequest((PhysicalAccountInfo)accountInfo));
			}
			return null;
		}
		
		private PhysicalStopListRequest getPhysicalStopListRequest(PhysicalAccountInfo accountInfo){
			PhysicalStopListRequest request = new PhysicalStopListRequest();
			request.setDocumentNumber(accountInfo.getDocumentNumber());
			request.setDocumentSeries(accountInfo.getDocumentSeries());
			request.setFirstname(accountInfo.getFirstname());
			request.setLastname(accountInfo.getLastname());
			request.setMiddlename(accountInfo.getMiddlename());
			return request;
		}

		private boolean processStopListStatus(StopListInfo stopListInfo)
				throws TransferException {
			if (StopListStatus.ASKSECURITY.equals(stopListInfo.getStatus())) {
				return false;
			}
			return true;
		}

		private void verifySrcBalance() throws TransferException {
			if (!accountService.verifyBalance(request.getSrcAccount()))
				throw new TransferException(LOW_BALANCE_ERROR_MESSAGE);
		}
	}

	public void setAccountService(AccountService accountService) {
		this.accountService = accountService;

	}

	public void setStopListService(StopListService stopListService) {
		this.stopListService = stopListService;
	}

	public void setOperationDao(OperationDao operationDao) {
		this.operationDao = operationDao;
	}
}
