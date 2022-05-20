import java.io.IOException;
import java.net.URI;
import java.net.URL;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import hapi.chart.ChartOuterClass.Chart;

import hapi.release.ReleaseOuterClass.Release;

import hapi.services.tiller.Tiller.InstallReleaseRequest;
import hapi.services.tiller.Tiller.InstallReleaseResponse;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;

import org.microbean.helm.ReleaseManager;
import org.microbean.helm.Tiller;

import org.microbean.helm.chart.URLChartLoader;

import org.yaml.snakeyaml.Yaml;

public class UpgradeService {
    public void runHelmUpgrade() throws IOException {
        final URI uri = URI.create("https://kubernetes-charts.storage.googleapis.com/wordpress-0.6.6.tgz");
        assert uri != null;
        final URL url = uri.toURL();
        assert url != null;
        Chart.Builder chart = null;
        try (final URLChartLoader chartLoader = new URLChartLoader()) {
            chart = chartLoader.load(url);
        }
        assert chart != null;

        try (final DefaultKubernetesClient client = new DefaultKubernetesClient();
             final Tiller tiller = new Tiller(client);
        final ReleaseManager releaseManager = new ReleaseManager(tiller)) {

            final InstallReleaseRequest.Builder requestBuilder = InstallReleaseRequest.newBuilder();
            assert requestBuilder != null;
            requestBuilder.setTimeout(300L);
            requestBuilder.setName("test-charts"); // Set the Helm release name
            requestBuilder.setWait(true); // Wait for Pods to be ready

// Create a structure that will hold user-supplied overriding values.
            final Map<String, Object> yaml = new LinkedHashMap<>();
            yaml.put("wordpressEmail", "sample@example.com");

// Convert it to a YAML string using SnakeYaml.
            final String yamlString = new Yaml().dump(yaml);

            // Set the user-supplied values in the only way that will be readable by
            // Tiller.  For some reason, Tiller itself only ever looks at the return
            // value of Config.Builder#getRaw(), and no other values-related "getter"
            // method.
            requestBuilder.getValuesBuilder().setRaw(yamlString);

// Install the loaded chart with the user-supplied overrides.
            final Future<InstallReleaseResponse> releaseFuture = releaseManager.install(requestBuilder, chart);
            assert releaseFuture != null;
            final Release release = releaseFuture.get().getRelease();
            assert release != null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}



