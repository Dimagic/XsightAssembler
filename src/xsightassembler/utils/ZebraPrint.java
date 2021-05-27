//package xsightassembler.utils;
//
//import javax.print.*;
//import javax.print.event.PrintJobAdapter;
//import javax.print.event.PrintJobEvent;
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.GregorianCalendar;
//import java.util.Objects;
//
//public class ZebraPrint {
//	private PrintService printService;
//	private Assembly assembly;
//	private String printerName;
//	private String template;
//
//	public ZebraPrint() {
//	}
//
//	public ZebraPrint(Assembly assembly, String printerName) {
//		this.printerName = printerName;
//		this.printService = getPrinterByName(printerName);
//		this.assembly = assembly;
//	}
//
//	public PrintService getPrintService() {
//		return printService;
//	}
//
//	public boolean printLabel() {
//		if (printService == null || assembly == null) {
//			MsgBox.msgWarning("Print label", "Print service or print job document is invalid.");
//			return false;
//		}
//		template = Objects.requireNonNull(Utils.getSettings()).getTemplate_area();
//		String currTemplate;
//		currTemplate = template.replaceAll("<SN>", assembly.getCaseSn());
//
//		StringBuffer buffer = new StringBuffer();
//		buffer.append(currTemplate);
//		runPrinting(buffer.toString());
//		return true;
//	}
//
//	public void printTemplate(String templ){
//		templ = templ.replaceAll("<DATE>", getDateTimeNow());
//		runPrinting(templ);
//	}
//
//	private void runPrinting(String s) {
//		try {
//			DocPrintJob job = getPrintService().createPrintJob();
//			job.addPrintJobListener(new ZebraJobListener());
//			byte[] by = s.getBytes();
//			DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
//			Doc doc = new SimpleDoc(by, flavor, null);
//			job.print(doc, null);
//		} catch (PrintException e) {
//			MsgBox.msgException(e);
//		}
//	}
//
//	private String getDateTimeNow(){
//		Date date = Calendar.getInstance().getTime();
//		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm");
//		return dateFormat.format(date);
//	}
//
//	private String getDateForPrint(Date date) {
//		Calendar calendar = new GregorianCalendar();
//		calendar.setTime(date);
//		int year = calendar.get(Calendar.YEAR);
//		int month = calendar.get(Calendar.MONTH) + 1;
//		int day = calendar.get(Calendar.DAY_OF_MONTH);
//		String y = Integer.toString(year).substring(2);
//		String m;
//		String d;
//		if (month < 10) {
//			m = String.format("0%s", Integer.toString(month));
//		} else {
//			m = Integer.toString(month);
//		}
//		if (day < 10) {
//			d = String.format("0%s", Integer.toString(day));
//		} else {
//			d = Integer.toString(day);
//		}
//		return String.format("%s%s%s", y, m, d);
//	}
//
//	private PrintService getPrinterByName(String name) {
//		PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
//		for (PrintService prService : printServices) {
//			if (prService.getName().toLowerCase().contains(name.toLowerCase())) {
//				return prService;
//			}
//		}
//		return null;
//	}
//
//	private static class JobCompleteMonitor extends PrintJobAdapter {
//		private boolean completed = false;
//
//		@Override
//		public void printJobCanceled(PrintJobEvent pje) {
//			signalCompletion();
//		}
//
//		@Override
//		public void printJobCompleted(PrintJobEvent pje) {
//			signalCompletion();
//		}
//
//		@Override
//		public void printJobFailed(PrintJobEvent pje) {
//			signalCompletion();
//		}
//
//		@Override
//		public void printJobNoMoreEvents(PrintJobEvent pje) {
//			signalCompletion();
//		}
//
//		private void signalCompletion() {
//			synchronized (JobCompleteMonitor.this) {
//				completed = true;
//				JobCompleteMonitor.this.notify();
//			}
//		}
//
//		public synchronized void waitForJobCompletion() {
//			try {
//				while (!completed) {
//					wait();
//				}
//			} catch (InterruptedException e) {
//
//			}
//		}
//	}
//
//}
