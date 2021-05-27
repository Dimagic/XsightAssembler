package xsightassembler.utils;

import javax.print.event.PrintJobEvent;
import javax.print.event.PrintJobListener;

public class ZebraJobListener implements PrintJobListener, MsgBox {
	public void printDataTransferCompleted(PrintJobEvent pje) {
		MsgBox.msgInfo("Printing", "The print data tranfer completed");
	}

	public void printJobCanceled(PrintJobEvent pje) {
		System.out.println("The print job was cancelled");
	}

	public void printJobCompleted(PrintJobEvent pje) {
		MsgBox.msgInfo("Printing", "The print was completed");
	}

	public void printJobFailed(PrintJobEvent pje) {
		MsgBox.msgError("Printing", "The print job has failed");
	}

	public void printJobNoMoreEvents(PrintJobEvent pje) {
		System.out.println("printJobNoMoreEvents");
	}

	public void printJobRequiresAttention(PrintJobEvent pje) {
	}
}
