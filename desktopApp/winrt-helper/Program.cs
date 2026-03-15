using System;
using System.Text;
using System.Threading.Tasks;
using Windows.Media.Control;
using Windows.Storage.Streams;

internal static class Program
{
    private static async Task<int> Main(string[] args)
    {
        Console.OutputEncoding = Encoding.UTF8;

        if (args.Length == 0)
        {
            WriteLine("ok=0");
            WriteLine("error=Missing command");
            return 2;
        }

        try
        {
            var command = args[0].Trim().ToLowerInvariant();
            return command switch
            {
                "get-state" => await HandleGetStateAsync(),
                "toggle" => await HandleControlAsync(ControlAction.Toggle),
                "next" => await HandleControlAsync(ControlAction.Next),
                "previous" => await HandleControlAsync(ControlAction.Previous),
                _ => HandleUnknown(command)
            };
        }
        catch (Exception ex)
        {
            WriteLine("ok=0");
            WriteLine($"error={Sanitize(ex.Message)}");
            return 1;
        }
    }

    private static int HandleUnknown(string command)
    {
        WriteLine("ok=0");
        WriteLine($"error=Unknown command: {Sanitize(command)}");
        return 2;
    }

    private static async Task<int> HandleGetStateAsync()
    {
        var manager = await GlobalSystemMediaTransportControlsSessionManager.RequestAsync();
        var session = manager.GetCurrentSession();

        if (session is null)
        {
            WriteLine("hasSession=0");
            return 0;
        }

        string title = string.Empty;
        string artist = string.Empty;
        string coverBase64 = "null";

        try
        {
            var properties = await session.TryGetMediaPropertiesAsync();
            if (properties is not null)
            {
                title = Sanitize(properties.Title);
                artist = Sanitize(properties.Artist);
                coverBase64 = await ReadThumbnailAsBase64SafeAsync(properties.Thumbnail);
            }
        }
        catch
        {
            // Keep partial state output even when media properties fail.
        }

        if (string.IsNullOrWhiteSpace(artist))
        {
            artist = Sanitize(session.SourceAppUserModelId);
        }

        var timeline = session.GetTimelineProperties();
        var playback = session.GetPlaybackInfo();

        WriteLine("hasSession=1");
        WriteLine($"title={title}");
        WriteLine($"artist={artist}");
        WriteLine($"durationMs={(long)timeline.EndTime.TotalMilliseconds}");
        WriteLine($"positionMs={(long)timeline.Position.TotalMilliseconds}");
        WriteLine($"isPlaying={(playback.PlaybackStatus == GlobalSystemMediaTransportControlsSessionPlaybackStatus.Playing ? "1" : "0")}");
        WriteLine($"coverBase64={coverBase64}");
        return 0;
    }

    private static async Task<int> HandleControlAsync(ControlAction action)
    {
        var manager = await GlobalSystemMediaTransportControlsSessionManager.RequestAsync();
        var session = manager.GetCurrentSession();

        if (session is null)
        {
            WriteLine("ok=0");
            WriteLine("error=No current media session");
            return 0;
        }

        bool ok = action switch
        {
            ControlAction.Toggle => await session.TryTogglePlayPauseAsync(),
            ControlAction.Next => await session.TrySkipNextAsync(),
            ControlAction.Previous => await session.TrySkipPreviousAsync(),
            _ => false
        };

        WriteLine($"ok={(ok ? "1" : "0")}");
        return 0;
    }

    private static async Task<string> ReadThumbnailAsBase64SafeAsync(IRandomAccessStreamReference? thumbnail)
    {
        try
        {
            if (thumbnail is null)
            {
                return "null";
            }

            using var stream = await thumbnail.OpenReadAsync();
            if (stream is null || stream.Size == 0)
            {
                return "null";
            }

            var length = stream.Size > int.MaxValue ? int.MaxValue : (int)stream.Size;
            using var input = stream.GetInputStreamAt(0);
            using var reader = new DataReader(input);
            await reader.LoadAsync((uint)length);

            var bytes = new byte[length];
            reader.ReadBytes(bytes);
            return Convert.ToBase64String(bytes);
        }
        catch
        {
            return "null";
        }
    }

    private static string Sanitize(string? value)
    {
        if (string.IsNullOrEmpty(value))
        {
            return string.Empty;
        }

        return value
            .Replace('\r', ' ')
            .Replace('\n', ' ')
            .Trim();
    }

    private static void WriteLine(string line)
    {
        Console.Out.WriteLine(line);
    }

    private enum ControlAction
    {
        Toggle,
        Next,
        Previous
    }
}
