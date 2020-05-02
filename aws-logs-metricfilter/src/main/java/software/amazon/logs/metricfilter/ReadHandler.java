package software.amazon.logs.metricfilter;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeMetricFiltersRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeMetricFiltersResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Objects;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudWatchLogsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        return proxy.initiate("AWS-Logs-MetricFilter::Read", proxyClient, model, callbackContext)
            .translateToServiceRequest(Translator::translateToReadRequest)
            .makeServiceCall((awsRequest, sdkProxyClient) -> readResource(awsRequest, sdkProxyClient , model))
            .done(this::constructResourceModelFromResponse);
    }

    private DescribeMetricFiltersResponse readResource(
        final DescribeMetricFiltersRequest awsRequest,
        final ProxyClient<CloudWatchLogsClient> proxyClient,
        final ResourceModel model) {
        DescribeMetricFiltersResponse awsResponse;
        try {

            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::describeMetricFilters);

        } catch (final ResourceNotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (final AwsServiceException e) {
            logger.log("Error trying to read resource: " + e.getMessage());
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }

        if (awsResponse.metricFilters().isEmpty()) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME,
                    Objects.toString(model.getPrimaryIdentifier()));
        }

        logger.log(String.format("%s has successfully been read." , ResourceModel.TYPE_NAME));
        return awsResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> constructResourceModelFromResponse(final DescribeMetricFiltersResponse awsResponse) {
        return ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(awsResponse));
    }
}
