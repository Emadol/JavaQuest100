using System;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Windows.Forms;

// Lanciatore autoestraente: estrae il pacchetto incorporato (una volta) in %LOCALAPPDATA% e avvia il gioco.
static class Stub
{
    const string BuildId = "@@BUILDID@@";

    [STAThread]
    static int Main()
    {
        try
        {
            string baseDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "JavaQuest100_app");
            string dest = Path.Combine(baseDir, BuildId);
            string exe = Path.Combine(dest, "JavaQuest100", "JavaQuest100.exe");
            if (!File.Exists(exe))
            {
                string tmp = dest + ".tmp";
                if (Directory.Exists(tmp)) Directory.Delete(tmp, true);
                Directory.CreateDirectory(tmp);
                using (Stream s = Assembly.GetExecutingAssembly().GetManifestResourceStream("payload.zip"))
                using (ZipArchive z = new ZipArchive(s, ZipArchiveMode.Read))
                {
                    foreach (ZipArchiveEntry e in z.Entries)
                    {
                        if (string.IsNullOrEmpty(e.Name)) { Directory.CreateDirectory(Path.Combine(tmp, e.FullName)); continue; }
                        string target = Path.GetFullPath(Path.Combine(tmp, e.FullName));
                        if (!target.StartsWith(Path.GetFullPath(tmp))) continue;
                        Directory.CreateDirectory(Path.GetDirectoryName(target));
                        e.ExtractToFile(target, true);
                    }
                }
                if (Directory.Exists(dest)) Directory.Delete(dest, true);
                Directory.Move(tmp, dest);
                // pulizia vecchie versioni
                try { foreach (string d in Directory.GetDirectories(baseDir)) if (!d.EndsWith(BuildId)) Directory.Delete(d, true); } catch { }
            }
            ProcessStartInfo psi = new ProcessStartInfo(exe) { WorkingDirectory = Path.GetDirectoryName(exe), UseShellExecute = false };
            Process.Start(psi);
            return 0;
        }
        catch (Exception ex)
        {
            MessageBox.Show("Impossibile avviare Java Quest 100:\n" + ex.Message, "Java Quest 100");
            return 1;
        }
    }
}
