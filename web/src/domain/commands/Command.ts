export interface Command {
  execute(): Promise<void>;
  undo?(): Promise<void>;
}

export class CommandInvoker {
  private history: Command[] = [];

  async execute(command: Command) {
    await command.execute();
    if (command.undo) {
      this.history.push(command);
    }
  }

  async undo() {
    const command = this.history.pop();
    if (command?.undo) {
      await command.undo();
    }
  }
}
