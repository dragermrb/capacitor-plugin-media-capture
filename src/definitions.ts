export interface MediaCapturePlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
