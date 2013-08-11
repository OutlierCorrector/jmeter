package org.apache.jmeter.threads;

import java.util.List;
import org.OutlierCorrector.*;

import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleListener;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.TestBeanHelper;
import org.apache.jmeter.testelement.TestElement;
import org.apache.log.Logger;

public class ListenerOutputHandler implements RequestHandler {
	public enum OutputLabelMode {
		COMPENSATE_UNDER_ORIGINAL_LABEL, COMPENSATE_AS_DUPLICATE_LABEL;
	}

	private List<SampleListener> listeners;
	//private SampleEvent originalEvent;
	private SampleResult originalResult;
	private Logger log;
	private final OutputLabelMode outputLabelMode;

	public ListenerOutputHandler(Logger log, OutputLabelMode outputLabelMode) {
		this.log = log;
		this.outputLabelMode = outputLabelMode;
	}

	public void setListeners(List<SampleListener> listeners) {
		this.listeners = listeners;
	}

	public void setOriginalEvent(SampleEvent event) {
		//this.originalEvent = event;
		this.originalResult = event.getResult();
	}

	public void handleOriginalRequest(Request request) {
		handleRequest(request, request.getRequestTypeLabel());
		if (outputLabelMode == OutputLabelMode.COMPENSATE_AS_DUPLICATE_LABEL) {
			handleRequest(request,
					(request.getRequestTypeLabel() + "-Compensated"));
		}
	}

	public void handleRequest(Request request) {
		if (request.isOriginalRequest()) {
			handleOriginalRequest(request);
		} else {
			if (outputLabelMode == OutputLabelMode.COMPENSATE_AS_DUPLICATE_LABEL) {
				handleRequest(request,
						(request.getRequestTypeLabel() + "-Compensated"));
			} else {
				handleRequest(request, request.getRequestTypeLabel());
			}
		}
	}
	
	public void flush() {
	}

	@SuppressWarnings("deprecation")
	// TestBeanHelper.prepare() is OK
	private void handleRequest(Request request, String label) {

		SampleResult newResult = new SampleResult(request.getTimeStamp(),
				(long) request.getResponseTime());
		newResult.setThreadName(request.getThreadName());
		newResult.setSampleLabel(label);
		newResult.setSuccessful(originalResult.isSuccessful());
		newResult.setBytes(originalResult.getBytes());
		SampleEvent res = new SampleEvent(newResult, newResult.getThreadName());

		for (SampleListener sampleListener : listeners) {
			try {
				TestBeanHelper.prepare((TestElement) sampleListener);

				sampleListener.sampleOccurred(res);
			}

			catch (RuntimeException e) {
				log.error("Detected problem in Listener: ", e);
				log.info("Continuing to process further listeners");
			}
		}
	}
}