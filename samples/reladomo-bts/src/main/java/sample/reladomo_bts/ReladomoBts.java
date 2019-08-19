package sample.reladomo_bts;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.gs.fw.common.mithra.MithraManager;
import com.gs.fw.common.mithra.MithraManagerProvider;

import yokohama.lang.test.Account;
import yokohama.lang.test.AccountFinder;
import yokohama.lang.test.Bug;
import yokohama.lang.test.BugFinder;
import yokohama.lang.test.BugProduct;
import yokohama.lang.test.Product;
import yokohama.lang.test.ProductList;
import yokohama.lang.test.ReportedBy;
import yokohama.lang.test.ReportedByFinder;

@Component
public class ReladomoBts implements CommandLineRunner {

	private static final int MAX_TRANSACTION_TIMEOUT = 120;

	MithraManager mithraManager = MithraManagerProvider.getMithraManager();

	@Override
	public void run(String... args) throws Exception {

		mithraManager.setTransactionTimeout(MAX_TRANSACTION_TIMEOUT);
		try (final InputStream is = getClass().getResourceAsStream("/ReladomoRuntimeConfig.xml")) {
			mithraManager.readConfiguration(is);
		}

		Account johnDoe = new Account();
		johnDoe.setAccountId(1);
		johnDoe.setAccountName("John Doe");
		johnDoe.setHourlyRate(BigDecimal.valueOf(123L));
		johnDoe.insert();

		Account janeDoe = new Account();
		janeDoe.setAccountId(2);
		janeDoe.setAccountName("Jane Doe");
		janeDoe.setHourlyRate(BigDecimal.valueOf(456L));
		janeDoe.insert();

		Bug bug1 = new Bug();
		bug1.setBugId(1);
		bug1.setDateReported(new Date(0L));
		bug1.setStatus("WORKING");
		bug1.insert();

		ReportedBy bug1IsReportedByJaneDoe = new ReportedBy();
		bug1IsReportedByJaneDoe.setBugId(bug1.getBugId());
		bug1IsReportedByJaneDoe.setAccountId(janeDoe.getAccountId());
		bug1IsReportedByJaneDoe.insert();

		String reporter1 = bug1.getReportedBy().getAccountName();
		System.out.println("bug #1 is reported by " + reporter1);

		// ------------------------

		Bug bug2 = new Bug();
		bug2.setBugId(2);
		bug2.setDateReported(new Date(0L));
		bug2.setStatus("CLOSED");
		bug2.insert();

		ReportedBy bug2IsReportedByJaneDoe = new ReportedBy();
		bug2IsReportedByJaneDoe.setBugId(bug2.getBugId());
		bug2IsReportedByJaneDoe.setAccountId(janeDoe.getAccountId());
		bug2IsReportedByJaneDoe.insert();

		ReportedByFinder.findMany(ReportedByFinder.all()).forEach(reportedBy -> {
			System.out
					.println("bug #" + reportedBy.getBugId() + " is reported by account #" + reportedBy.getAccountId());
		});

		// ------------------------

		Product bts = new Product();
		bts.setProductId(1);
		bts.setProductName("Bug Tracking System");
		bts.insert();

		Product vcs = new Product();
		vcs.setProductId(2);
		vcs.setProductName("Version Control System");
		vcs.insert();

		BugProduct bp1 = new BugProduct();
		bp1.setBugId(1);
		bp1.setProductId(1);
		bp1.insert();

		BugProduct bp2 = new BugProduct();
		bp2.setBugId(1);
		bp2.setProductId(2);
		bp2.insert();

		BugProduct bp3 = new BugProduct();
		bp3.setBug(bug2);
		bp3.setProduct(vcs);
		bp3.insert();

		for (Bug bug : BugFinder.findMany(BugFinder.all())) {
			ProductList products = bug.getBugProduct();
			System.out.println("bug #" + bug.getBugId() + " is related to the following products: "
					+ products.stream().map(Product::getProductName).collect(Collectors.joining(", ")));
		}
	}

}
